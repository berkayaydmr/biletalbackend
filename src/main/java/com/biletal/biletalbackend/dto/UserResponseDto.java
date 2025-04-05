package com.biletal.biletalbackend.dto;

import com.biletal.biletalbackend.model.User;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponseDto {
    Long id;
    String firstName;
    String lastName;
    String email;
    String birthDate;
    String birthCountry;
    String gender ;
    String birthCity;
    String address;
    String phone;
    String role;

    public static UserResponseDto fromUser(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getBirthDate().toString(),
                user.getBirthCountry(),
                user.getGender().name(),
                user.getBirthCity(),
                user.getAddress(),
                user.getPhone(),
                user.getRole().name()
        );
    }
}