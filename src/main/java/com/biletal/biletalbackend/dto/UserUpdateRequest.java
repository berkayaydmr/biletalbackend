package com.biletal.biletalbackend.dto;

import org.springframework.beans.factory.annotation.Value;

import com.biletal.biletalbackend.custenum.Gender;
import com.fasterxml.jackson.databind.annotation.EnumNaming;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {

    @Size(min = 2, message = "Ad en az 2 karakter olmalıdır")
    private String firstName;
    
    @Size(min = 2, message = "Soyad en az 2 karakter olmalıdır")
    private String lastName;
    
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    private String email;
    
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    @Pattern(regexp = ".*[A-Z].*", message = "Şifre en az bir büyük harf içermelidir")
    @Pattern(regexp = ".*[a-z].*", message = "Şifre en az bir küçük harf içermelidir")
    @Pattern(regexp = ".*\\d.*", message = "Şifre en az bir rakam içermelidir")
    private String password;
    
    @Pattern(regexp = "^(\\+90|0)?[0-9]{10}$", message = "Geçerli bir telefon numarası giriniz")
    private String phone;

    @Size(min = 2, message = "Ülke adı en az 2 karakter olmalıdır")
    private String birthCountry;

    @Size(min = 2, message = "Şehir adı en az 2 karakter olmalıdır")
    private String birthCity;

    private String gender;

    @Size(min = 10, message = "Doğum tarihi en az 10 karakter olmalıdır")
    private String birthDate;

    private String adress;
}