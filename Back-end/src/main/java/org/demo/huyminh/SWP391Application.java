package org.demo.huyminh;

import lombok.extern.slf4j.Slf4j;
import org.demo.huyminh.Entity.User;
import org.demo.huyminh.Entity.Role;
import org.demo.huyminh.Repository.RoleRepository;
import org.demo.huyminh.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@SpringBootApplication
@EnableFeignClients
@Slf4j
public class SWP391Application {

    public static void main(String[] args) {
        SpringApplication.run(SWP391Application.class, args);
    }

    @Autowired
    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository) {
        return args -> {
            if (userRepository.findByUsername("admin1").isPresent()) {
                return;
            }

            Role userRole = roleRepository.save(Role.builder()
                    .name("USER")
                    .description("User role")
                    .build());

            Role adminRole = roleRepository.save(Role.builder()
                    .name("ADMIN")
                    .description("Admin role")
                    .build());

            Role volunteerRole = roleRepository.save(Role.builder()
                    .name("VOLUNTEER")
                    .description("Volunteer role")
                    .build());

            Role shelterRole = roleRepository.save(Role.builder()
                    .name("SHELTER_STAFF")
                    .description("Shelter role")
                    .build());

            Role donorRole = roleRepository.save(Role.builder()
                    .name("DONOR")
                    .description("Donor role")
                    .build());

            roleRepository.saveAll(List.of(userRole, adminRole, volunteerRole, shelterRole, donorRole));

            var roles = new HashSet<Role>();
            roles.add(adminRole);
            roles.add(userRole);
            roles.add(volunteerRole);


            User user = User.builder()
                    .username("admin1")
                    .password(passwordEncoder.encode("123456"))
                    .firstname("Minh")
                    .lastname("Nguyen")
                    .id(UUID.randomUUID().toString())
                    .email("hoangvo2122@gmail.com")
                    .roles(roles)
                    .isEnabled(true)
                    .isPasswordChangeable(false)
                    .build();

            userRepository.save(user);
            log.warn("Admin user has been created with password: admin. Please change it!");
        };
    }
}
