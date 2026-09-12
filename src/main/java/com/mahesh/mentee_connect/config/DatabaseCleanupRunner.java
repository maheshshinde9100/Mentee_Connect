package com.mahesh.mentee_connect.config;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs once at startup to fix corrupted / inconsistent documents in MongoDB.
 *
 * IMPORTANT: MongoDB Atlas 5+ does NOT support $ref/$id as query operators.
 * All queries use the dot-notation "assignedMentor.$id" (ObjectId) instead.
 *
 * Problems fixed:
 *  1. Mentor docs missing the "role" field → patched to ROLE_MENTOR
 *  2. Student docs missing the "role" field → patched to ROLE_STUDENT
 *  3. Students whose assignedMentor.$id points to a non-existent mentor → cleared
 *  4. Orphan students (no assignedMentor) → auto-assigned to a mentor with free slots
 *  5. Mentor.assignedStudents list out-of-sync → rebuilt from Student side
 */
@Component
public class DatabaseCleanupRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCleanupRunner.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        log.info("===== DatabaseCleanupRunner: starting production data repair =====");
        try {
            fixMissingRoles();
        } catch (Exception e) {
            log.error("fixMissingRoles failed: ", e);
        }
        try {
            fixBrokenStudentRefs();
        } catch (Exception e) {
            log.error("fixBrokenStudentRefs failed: ", e);
        }
        try {
            syncMentorStudentLists();
        } catch (Exception e) {
            log.error("syncMentorStudentLists failed: ", e);
        }
        try {
            autoAssignOrphanStudents();
        } catch (Exception e) {
            log.error("autoAssignOrphanStudents failed: ", e);
        }
        log.info("===== DatabaseCleanupRunner: finished =====");
    }

    // ─── 1. Patch missing role fields ──────────────────────────────────────────

    private void fixMissingRoles() {
        Query noRoleMentors = new Query(Criteria.where("role").exists(false));
        long m = mongoTemplate.updateMulti(noRoleMentors,
                new Update().set("role", "ROLE_MENTOR"), Document.class, "mentors")
                .getModifiedCount();

        Query noRoleStudents = new Query(Criteria.where("role").exists(false));
        long s = mongoTemplate.updateMulti(noRoleStudents,
                new Update().set("role", "ROLE_STUDENT"), Document.class, "students")
                .getModifiedCount();

        if (m > 0 || s > 0) {
            log.info("fixMissingRoles: patched {} mentor(s) and {} student(s)", m, s);
        } else {
            log.info("fixMissingRoles: all documents already have a role — OK");
        }
    }

    // ─── 2. Clear dangling assignedMentor references ───────────────────────────

    private void fixBrokenStudentRefs() {
        // Collect all valid mentor _id values as ObjectId
        List<Document> mentorDocs = mongoTemplate.getCollection("mentors")
                .find(new Document())
                .projection(new Document("_id", 1))
                .into(new ArrayList<>());

        Set<ObjectId> validMentorIds = mentorDocs.stream()
                .map(d -> d.getObjectId("_id"))
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        log.info("fixBrokenStudentRefs: {} valid mentors in DB", validMentorIds.size());

        // Find students that have an assignedMentor field
        List<Document> studentDocs = mongoTemplate.getCollection("students")
                .find(new Document("assignedMentor", new Document("$exists", true)))
                .into(new ArrayList<>());

        int cleared = 0;
        for (Document student : studentDocs) {
            Object assignedMentor = student.get("assignedMentor");
            if (!(assignedMentor instanceof Document)) continue;

            Document ref = (Document) assignedMentor;
            // In MongoDB Atlas, DBRefs are stored as { $ref, $id, $db }
            // Use dot-notation for query but here we just read the stored $id field
            Object rawId = ref.get("$id");
            if (rawId == null) {
                // Malformed ref — clear it
                mongoTemplate.getCollection("students").updateOne(
                        new Document("_id", student.get("_id")),
                        new Document("$unset", new Document("assignedMentor", "")));
                log.warn("Cleared malformed assignedMentor from student {}", student.get("_id"));
                cleared++;
                continue;
            }

            ObjectId mentorOid = null;
            if (rawId instanceof ObjectId) {
                mentorOid = (ObjectId) rawId;
            } else {
                try { mentorOid = new ObjectId(rawId.toString()); } catch (Exception ignored) {}
            }

            if (mentorOid != null && !validMentorIds.contains(mentorOid)) {
                mongoTemplate.getCollection("students").updateOne(
                        new Document("_id", student.get("_id")),
                        new Document("$unset", new Document("assignedMentor", "")));
                log.warn("Cleared dangling mentor ref {} from student {}", mentorOid, student.get("_id"));
                cleared++;
            }
        }
        log.info("fixBrokenStudentRefs: cleared {} dangling reference(s)", cleared);
    }

    // ─── 3. Rebuild Mentor.assignedStudents from Student.assignedMentor ────────

    private void syncMentorStudentLists() {
        List<Document> mentorDocs = mongoTemplate.getCollection("mentors")
                .find(new Document())
                .projection(new Document("_id", 1).append("assignedStudents", 1))
                .into(new ArrayList<>());

        int synced = 0;
        for (Document mentor : mentorDocs) {
            ObjectId mentorOid = mentor.getObjectId("_id");
            if (mentorOid == null) continue;

            // ✅ Atlas-compatible: query using dot-notation "assignedMentor.$id"
            // NOT "assignedMentor.$ref" — that's not a valid query operator in Atlas
            List<Document> assignedStudentDocs = mongoTemplate.getCollection("students")
                    .find(new Document("assignedMentor.$id", mentorOid))
                    .projection(new Document("_id", 1))
                    .into(new ArrayList<>());

            // Build the correct DBRef list for Mentor.assignedStudents
            List<Document> studentRefs = assignedStudentDocs.stream()
                    .map(s -> new Document("$ref", "students").append("$id", s.getObjectId("_id")))
                    .collect(Collectors.toList());

            List<?> currentRefs = mentor.getList("assignedStudents", Object.class);
            int currentSize = currentRefs == null ? 0 : currentRefs.size();

            if (currentSize != studentRefs.size()) {
                mongoTemplate.getCollection("mentors").updateOne(
                        new Document("_id", mentorOid),
                        new Document("$set", new Document("assignedStudents", studentRefs)));
                log.info("syncMentorStudentLists: mentor {} rebuilt ({} → {})",
                        mentorOid, currentSize, studentRefs.size());
                synced++;
            }
        }
        log.info("syncMentorStudentLists: synced {} mentor document(s)", synced);
    }

    // ─── 4. Auto-assign orphan students to available mentors ───────────────────

    private void autoAssignOrphanStudents() {
        // Find students with no assignedMentor field at all
        List<Document> orphanStudents = mongoTemplate.getCollection("students")
                .find(new Document("assignedMentor", new Document("$exists", false)))
                .into(new ArrayList<>());

        if (orphanStudents.isEmpty()) {
            log.info("autoAssignOrphanStudents: no orphan students found — OK");
            return;
        }

        log.info("autoAssignOrphanStudents: {} orphan student(s) found", orphanStudents.size());

        List<Document> mentorDocs = mongoTemplate.getCollection("mentors")
                .find(new Document())
                .into(new ArrayList<>());

        if (mentorDocs.isEmpty()) {
            log.warn("autoAssignOrphanStudents: no mentors in DB — cannot assign");
            return;
        }

        int assigned = 0;
        for (Document student : orphanStudents) {
            ObjectId studentOid = student.getObjectId("_id");
            if (studentOid == null) continue;

            // Find a mentor with a free slot
            Document selectedMentor = null;
            for (Document mentor : mentorDocs) {
                ObjectId mentorOid = mentor.getObjectId("_id");
                if (mentorOid == null) continue;
                int maxStudents = mentor.getInteger("maxStudents", 10);

                // ✅ Atlas-compatible dot-notation count
                long count = mongoTemplate.getCollection("students")
                        .countDocuments(new Document("assignedMentor.$id", mentorOid));

                if (count < maxStudents) {
                    selectedMentor = mentor;
                    break;
                }
            }

            if (selectedMentor == null) {
                log.warn("autoAssignOrphanStudents: no mentor with free slot for student {}", studentOid);
                continue;
            }

            ObjectId mentorOid = selectedMentor.getObjectId("_id");

            // Write the DBRef as MongoDB stores it: { $ref, $id }
            Document mentorRef = new Document("$ref", "mentors").append("$id", mentorOid);
            mongoTemplate.getCollection("students").updateOne(
                    new Document("_id", studentOid),
                    new Document("$set", new Document("assignedMentor", mentorRef)));

            // Keep mentor's assignedStudents in sync
            Document studentRef = new Document("$ref", "students").append("$id", studentOid);
            mongoTemplate.getCollection("mentors").updateOne(
                    new Document("_id", mentorOid),
                    new Document("$addToSet", new Document("assignedStudents", studentRef)));

            log.info("autoAssignOrphanStudents: assigned student {} → mentor {}", studentOid, mentorOid);
            assigned++;
        }

        log.info("autoAssignOrphanStudents: assigned {}/{} student(s)", assigned, orphanStudents.size());
    }
}
