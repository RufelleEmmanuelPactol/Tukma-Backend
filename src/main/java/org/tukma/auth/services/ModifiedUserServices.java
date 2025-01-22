package org.tukma.auth.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.tukma.auth.exceptions.NullUserException;
import org.tukma.auth.models.UserEntity;
import org.tukma.auth.repositories.UserRepository;

@Service
public class ModifiedUserServices implements org.springframework.security.core.userdetails.UserDetailsService {


    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Autowired
    public ModifiedUserServices(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User of string-id:`" + username + "` not found."));
    }

    public boolean userExists(String email) {
        return userRepository.findByUsername(email).isPresent();
    }

    public UserEntity createUser(String email, String password, String firstName, String lastName, boolean isApplicant) {
        UserEntity user = new UserEntity();
        user.setUsername(email);
        user.setPassword(hashPassword(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRecruiter(!isApplicant);
        return userRepository.save(user);

    }

}
