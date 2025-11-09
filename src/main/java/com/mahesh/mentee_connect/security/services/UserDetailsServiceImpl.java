package com.mahesh.mentee_connect.security.services;

import com.mahesh.mentee_connect.model.User;
import com.mahesh.mentee_connect.repository.StudentRepository;
import com.mahesh.mentee_connect.repository.MentorRepository;
import com.mahesh.mentee_connect.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private MentorRepository mentorRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Try to find by email first
        Optional<User> user = studentRepository.findByEmail(usernameOrEmail)
                .map(student -> (User) student)
                .or(() -> mentorRepository.findByEmail(usernameOrEmail)
                        .map(mentor -> (User) mentor))
                .or(() -> adminRepository.findByEmail(usernameOrEmail)
                        .map(admin -> (User) admin));
                    
        // If not found by email, try by username
        if (user.isEmpty()) {
            user = studentRepository.findByUsername(usernameOrEmail)
                    .map(student -> (User) student)
                    .or(() -> mentorRepository.findByUsername(usernameOrEmail)
                            .map(mentor -> (User) mentor))
                    .or(() -> adminRepository.findByUsername(usernameOrEmail)
                            .map(admin -> (User) admin));
        }

        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User Not Found");
        }

        return UserDetailsImpl.build(user.get());
    }
} 