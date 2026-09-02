// StudentRepository.java
package com.mahesh.mentee_connect.repository;

import com.mahesh.mentee_connect.model.Student;
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

    @Query("{'assignedMentor' : { '$ref' : 'mentors' , '$id' : { '$oid' : ?0 } }}")
    List<Student> findByAssignedMentorId(String mentorId);

    @Query("{'assignedMentor' : { '$ref' : 'mentors' , '$id' : { '$oid' : ?0 } }}")
    Page<Student> findByAssignedMentorId(String mentorId, Pageable pageable);

    @Query("{'assignedMentor.$id': { $oid: ?0 }}")
    long countByAssignedMentorId(String mentorId);

    List<Student> findByFirstNameContainingOrLastNameContainingOrEmailContaining(
        String firstName, String lastName, String email);
    List<Student> findByBatch(String batchName);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByStudentId(String studentId);
}
