package com.biletal.biletalbackend.dto;

import java.time.LocalDate;
import com.biletal.biletalbackend.custenum.Gender;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;

@Data
public class RegistrationRequest {
    @NotBlank(message = "İsim alanı zorunludur")
    private String firstName;
    
    @NotBlank(message = "Soyisim alanı zorunludur")
    private String lastName;
    
    @NotNull(message = "Doğum tarihi zorunludur")
    @Past(message = "Doğum tarihi geçmiş bir tarih olmalıdır")
    private LocalDate birthDate;
    
    @NotBlank(message = "Doğum ülkesi zorunludur")
    private String birthCountry;
    
    @NotBlank(message = "Doğum şehri zorunludur")
    private String birthCity;
    
    @NotNull(message = "Cinsiyet zorunludur")
    private String gender;
    
    @NotBlank(message = "E-posta adresi zorunludur")
    @Email(message = "Geçerli bir e-posta adresi girin")
    private String email;
    
    // Optional fields
    private String address;
    private String phone;
}