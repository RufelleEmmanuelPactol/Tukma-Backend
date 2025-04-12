package org.tukma.auth.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import net.minidev.json.annotate.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Setter
@Getter
@JsonIgnoreProperties({"password", "enabled", "authorities"})
public class UserEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "email cannot be empty.")
    @Column(nullable = false, unique = true)
    private String username;

    private String firstName;
    public String lastName;

    @Column(nullable = true)
    private String companyName;

    @JsonIgnore
    private String password;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean isRecruiter;

    @Column(nullable = true)
    private Boolean hasJob;




    @Override
    public Collection<? extends GrantedAuthority>getAuthorities() {
        return List.of();
    }


    public @NotBlank(message = "email cannot be empty.") String getUsername() {
        return this.username;
    }

    public void setUsername(@NotBlank(message = "email cannot be empty.") String username) {
        this.username = username;
    }
}
