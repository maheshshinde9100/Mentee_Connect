// StudentRepository.java
package com.mahesh.mentee_connect.repository;

import com.mahesh.mentee_connect.model.Student;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends MongoRepository<Student, String> {
    Optional<Student> findByEmail(String email);
    Optional<Student> findByUsername(String username);
    Optional<Student> findByStudentId(String studentId);

    // Atlas-compatible: query DBRef using dot-notation with ObjectId parameter.
    // Spring Data MongoDB converts ObjectId directly — no $oid shell syntax needed.
    @Query(value = "{ 'assignedMentor.$id' : ?0 }")
    List<Student> findByAssignedMentorId(ObjectId mentorId);

    @Query(value = "{ 'assignedMentor.$id' : ?0 }")
    Page<Student> findByAssignedMentorId(ObjectId mentorId, Pageable pageable);

    @Query(value = "{ 'assignedMentor.$id' : ?0 }", count = true)
    long countByAssignedMentorId(ObjectId mentorId);

    List<Student> findByFirstNameContainingOrLastNameContainingOrEmailContaining(
        String firstName, String lastName, String email);
    List<Student> findByBatch(String batchName);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByStudentId(String studentId);
}
