package com.mahesh.mentee_connect.service.impl;

import com.mahesh.mentee_connect.model.*;
import com.mahesh.mentee_connect.repository.*;
import com.mahesh.mentee_connect.service.AnalyticsService;
import com.mahesh.mentee_connect.payload.response.AnalyticsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final String UNASSIGNED = "Unassigned";
    private static final String UNSPECIFIED = "Not specified";

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private MentorRepository mentorRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "analytics", key = "'mentorEngagement'")
    public MentorEngagementStats getMentorEngagementStats() {
        Map<String, List<Student>> studentsByMentor = groupStudentsByMentor(studentRepository.findAll());
        return new MentorEngagementStats(
                mentorRepository.findAll().stream()
                        .map(mentor -> new MentorStats(
                                mentorKey(mentor),
                                displayName(mentor.getFirstName(), mentor.getLastName()),
                                studentsByMentor.getOrDefault(mentor.getId(), Collections.emptyList())))
                        .collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "analytics", key = "'studentProgress'")
    public List<StudentProgressStats> getStudentProgressStats() {
        return studentRepository.findAll().stream()
                .map(student -> new StudentProgressStats(
                        student.getId(),
                        displayName(student.getFirstName(), student.getLastName()),
                        student.getAssignedMentor() != null ? "ACTIVE" : "INACTIVE",
                        student.getAttendance(),
                        student.getCgpa()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "analytics", key = "'userCount'")
    public AnalyticsResponse.UserCountStats getUserCountStats() {
        List<Student> students = studentRepository.findAll();
        List<Mentor> mentors = mentorRepository.findAll();
        List<Admin> admins = adminRepository.findAll();

        long activeStudents = students.stream().filter(Student::isActive).count();
        long activeMentors = mentors.stream().filter(Mentor::isActive).count();
        long activeAdmins = admins.stream().filter(Admin::isActive).count();

        return AnalyticsResponse.UserCountStats.builder()
                .totalUsers(students.size() + mentors.size() + admins.size())
                .totalStudents(students.size())
                .totalMentors(mentors.size())
                .totalAdmins(admins.size())
                .activeUsers(activeStudents + activeMentors + activeAdmins)
                .inactiveUsers((students.size() + mentors.size() + admins.size()) -
                        (activeStudents + activeMentors + activeAdmins))
                .build();
    }

    @Override
    @Cacheable(value = "analytics", key = "'department'")
    public AnalyticsResponse.DepartmentStats getDepartmentStats() {
        List<Student> students = studentRepository.findAll();
        List<Mentor> mentors = mentorRepository.findAll();

        Map<String, Long> studentsPerDepartment = students.stream()
                .collect(Collectors.groupingBy(
                        student -> departmentKey(student, UNASSIGNED),
                        Collectors.counting()));

        Map<String, Long> mentorsPerDepartment = mentors.stream()
                .collect(Collectors.groupingBy(
                        mentor -> fallback(mentor.getDepartment(), UNASSIGNED),
                        Collectors.counting()));

        Map<String, Double> averageCgpaByDepartment = students.stream()
                .collect(Collectors.groupingBy(
                        student -> departmentKey(student, UNASSIGNED),
                        Collectors.averagingDouble(Student::getCgpa)));

        List<String> topPerformingDepartments = averageCgpaByDepartment.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return AnalyticsResponse.DepartmentStats.builder()
                .studentsPerDepartment(studentsPerDepartment)
                .mentorsPerDepartment(mentorsPerDepartment)
                .averageCgpaByDepartment(averageCgpaByDepartment)
                .topPerformingDepartments(topPerformingDepartments)
                .build();
    }

    @Override
    @Cacheable(value = "analytics", key = "'student'")
    public AnalyticsResponse.StudentStats getStudentStats() {
        List<Student> students = studentRepository.findAll();

        Map<String, Long> studentsByBatch = students.stream()
                .collect(Collectors.groupingBy(
                        student -> fallback(student.getBatch(), UNASSIGNED),
                        Collectors.counting()));

        Map<Integer, Long> studentsBySemester = students.stream()
                .filter(student -> student.getSemester() > 0)
                .collect(Collectors.groupingBy(
                        Student::getSemester,
                        Collectors.counting()));

        double averageCgpa = students.stream()
                .mapToDouble(Student::getCgpa)
                .average()
                .orElse(0.0);

        Map<String, Long> studentsPerMentor = students.stream()
                .filter(s -> s.getAssignedMentor() != null)
                .collect(Collectors.groupingBy(
                        s -> mentorKey(s.getAssignedMentor()),
                        Collectors.counting()));

        Map<String, Double> attendanceByBatch = students.stream()
                .collect(Collectors.groupingBy(
                        student -> fallback(student.getBatch(), UNASSIGNED),
                        Collectors.averagingDouble(Student::getAttendance)));

        return AnalyticsResponse.StudentStats.builder()
                .studentsByBatch(studentsByBatch)
                .studentsBySemester(studentsBySemester)
                .averageCgpa(averageCgpa)
                .studentsPerMentor(studentsPerMentor)
                .attendanceByBatch(attendanceByBatch)
                .build();
    }

    @Override
    @Cacheable(value = "analytics", key = "'mentor'")
    public AnalyticsResponse.MentorStats getMentorStats() {
        List<Mentor> mentors = mentorRepository.findAll();
        Map<String, List<Student>> studentsByMentor = groupStudentsByMentor(studentRepository.findAll());

        Map<String, Long> mentorsByExperience = mentors.stream()
                .collect(Collectors.groupingBy(
                        mentor -> experienceRange(mentor.getYearsOfExperience()),
                        Collectors.counting()));

        Map<String, Long> mentorsBySpecialization = mentors.stream()
                .collect(Collectors.groupingBy(
                        mentor -> fallback(mentor.getSpecialization(), UNSPECIFIED),
                        Collectors.counting()));

        double averageStudentsPerMentor = mentors.stream()
                .mapToInt(mentor -> studentsByMentor.getOrDefault(mentor.getId(), Collections.emptyList()).size())
                .average()
                .orElse(0.0);

        List<AnalyticsResponse.MentorPerformance> topPerformingMentors = mentors.stream()
                .map(mentor -> calculateMentorPerformance(
                        mentor,
                        studentsByMentor.getOrDefault(mentor.getId(), Collections.emptyList())))
                .sorted(Comparator.comparingDouble(AnalyticsResponse.MentorPerformance::getAverageStudentCgpa)
                        .reversed())
                .limit(5)
                .collect(Collectors.toList());

        return AnalyticsResponse.MentorStats.builder()
                .mentorsByExperience(mentorsByExperience)
                .mentorsBySpecialization(mentorsBySpecialization)
                .averageStudentsPerMentor(averageStudentsPerMentor)
                .topPerformingMentors(topPerformingMentors)
                .build();
    }

    @Override
    @Cacheable(value = "analytics", key = "'allAnalytics'")
    public AnalyticsResponse getAllAnalytics() {
        return AnalyticsResponse.builder()
                .userCounts(getUserCountStats())
                .departmentStats(getDepartmentStats())
                .studentStats(getStudentStats())
                .mentorStats(getMentorStats())
                .build();
    }

    private String experienceRange(int years) {
        if (years < 2)
            return "0-2 years";
        if (years < 5)
            return "2-5 years";
        if (years < 10)
            return "5-10 years";
        return "10+ years";
    }

    private AnalyticsResponse.MentorPerformance calculateMentorPerformance(Mentor mentor, List<Student> assignedStudents) {
        List<Student> students = assignedStudents == null ? Collections.emptyList() : assignedStudents;

        double avgCgpa = students.stream()
                .mapToDouble(Student::getCgpa)
                .average()
                .orElse(0.0);

        double avgAttendance = students.stream()
                .mapToDouble(Student::getAttendance)
                .average()
                .orElse(0.0);

        return AnalyticsResponse.MentorPerformance.builder()
                .mentorId(mentorKey(mentor))
                .mentorName(displayName(mentor.getFirstName(), mentor.getLastName()))
                .studentCount(students.size())
                .averageStudentCgpa(avgCgpa)
                .averageStudentAttendance(avgAttendance)
                .build();
    }

    /**
     * Groups students by their assigned mentor's document id. Reading the mentor
     * reference's id does not trigger a lazy DBRef load, so this stays a single query.
     */
    private Map<String, List<Student>> groupStudentsByMentor(List<Student> students) {
        Map<String, List<Student>> grouped = new HashMap<>();
        for (Student student : students) {
            Mentor mentor = student.getAssignedMentor();
            if (mentor == null || mentor.getId() == null) {
                continue;
            }
            grouped.computeIfAbsent(mentor.getId(), key -> new ArrayList<>()).add(student);
        }
        return grouped;
    }

    private String departmentKey(Student student, String fallbackValue) {
        if (student.getDepartment() != null && !student.getDepartment().isBlank()) {
            return student.getDepartment().trim();
        }
        return fallback(student.getCourse(), fallbackValue);
    }

    private String mentorKey(Mentor mentor) {
        String key = mentor.getMentorId();
        if (key == null || key.isBlank()) {
            key = mentor.getId();
        }
        return fallback(key, UNASSIGNED);
    }

    private String fallback(String value, String fallbackValue) {
        return value == null || value.isBlank() ? fallbackValue : value.trim();
    }

    private String displayName(String firstName, String lastName) {
        String name = ((firstName == null ? "" : firstName.trim()) + " "
                + (lastName == null ? "" : lastName.trim())).trim();
        return name.isEmpty() ? UNASSIGNED : name;
    }
}
