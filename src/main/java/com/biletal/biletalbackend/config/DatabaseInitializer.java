package com.biletal.biletalbackend.config;

import com.biletal.biletalbackend.custenum.Gender;
import com.biletal.biletalbackend.custenum.Role;
import com.biletal.biletalbackend.model.User;
import com.biletal.biletalbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.firstname}")
    private String adminFirstName;

    @Value("${admin.lastname}")
    private String adminLastName;
    
    @Value("${admin.birthdate}")
    private String adminBirthDate;
    
    @Value("${admin.birthcountry}")
    private String adminBirthCountry;
    
    @Value("${admin.birthcity}")
    private String adminBirthCity;
    
    @Value("${admin.gender}")
    private String adminGender;
    
    @Value("${admin.address:Biletal HQ}")
    private String adminAddress;
    
    @Value("${admin.phone:+905555555555}")
    private String adminPhone;

    public DatabaseInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Check if SYSTEM_ADMIN user exists
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            logger.info("Sistem yönetici kullanıcısı oluşturuluyor: {}", adminEmail);
            
            User adminUser = new User();
            adminUser.setFirstName(adminFirstName);
            adminUser.setLastName(adminLastName);
            adminUser.setBirthDate(LocalDate.parse(adminBirthDate));
            adminUser.setBirthCountry(adminBirthCountry);
            adminUser.setBirthCity(adminBirthCity);
            adminUser.setGender(Gender.valueOf(adminGender));
            adminUser.setAddress(adminAddress);
            adminUser.setPhone(adminPhone);
            adminUser.setEmail(adminEmail);
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            adminUser.setRole(Role.SYSTEM_ADMIN);
            
            userRepository.save(adminUser);
            
            logger.info("Sistem yönetici kullanıcısı başarıyla oluşturuldu");
        } else {
            logger.info("Sistem yönetici kullanıcısı zaten mevcut");
        }

        String usermail = "wp.aydemir@hotmail.com";

        if (userRepository.findByEmail(usermail).isEmpty()) {
            logger.info("Sistem yönetici kullanıcısı oluşturuluyor: {}", adminEmail);
            
            User user = new User();
            user.setFirstName("Berkay");
            user.setLastName("Aydemir");
            user.setBirthDate(LocalDate.parse("2002-01-01"));
            user.setBirthCountry("Türkiye");
            user.setBirthCity("Istanbul");
            user.setGender(Gender.MALE);
            user.setAddress("Biletal HQ");
            user.setEmail(usermail);
            user.setPassword(passwordEncoder.encode("AaDdFf1234"));
            user.setRole(Role.END_USER);
            
            userRepository.save(user);
            
            logger.info("Kullanıcı başarıyla oluşturuldu");
        } else {
            logger.info("Kullanıcı zaten mevcut");
        }
    }
}
