package org.tukma.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class SignUpDto {
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setApplicant(boolean applicant) {this.isApplicant = applicant;}

    public boolean isApplicant() {
        return isApplicant;
    }

    @NotBlank(message = "email is required.")
    @NotNull
    private String email;
    @NotBlank(message = "password is required.")
    @NotNull
    private String password;
    @NotBlank(message = "first name is required.")
    @NotNull
    private String firstName;
    @NotBlank(message = "last name is required.")
    @NotNull
    private String lastName;


    private boolean isApplicant;

    private String companyName;

}