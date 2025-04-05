package com.biletal.biletalbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUpdateRequest {

    @NotBlank(message = "Ad alanı zorunludur")
    private String firstName;
    
    @NotBlank(message = "Soyad alanı zorunludur")
    private String lastName;
    
    @NotBlank(message = "E-posta alanı zorunludur")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    private String email;
    
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    @Pattern(regexp = ".*[A-Z].*", message = "Şifre en az bir büyük harf içermelidir")
    @Pattern(regexp = ".*[a-z].*", message = "Şifre en az bir küçük harf içermelidir")
    @Pattern(regexp = ".*\\d.*", message = "Şifre en az bir rakam içermelidir")
    private String password;
    
    @Pattern(regexp = "^(\\+90|0)?[0-9]{10}$", message = "Geçerli bir telefon numarası giriniz")
    private String phone;
}