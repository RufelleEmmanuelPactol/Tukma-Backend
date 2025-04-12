package org.tukma.auth.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.tukma.auth.exceptions.NullUserException;
import org.tukma.auth.models.UserEntity;
import org.tukma.auth.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

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

    /**
     * Save an existing user to the database
     * @param user The user entity to save
     * @return The saved user entity
     */
    public UserEntity saveUser(UserEntity user) {
        return userRepository.save(user);
    }
    
    /**
     * Update the hasJob field for a specific user by ID
     * @param userId The ID of the user to update
     * @param hasJob The new hasJob value
     * @return true if the user was found and updated, false otherwise
     */
    public boolean updateUserJobStatus(Long userId, Boolean hasJob) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            user.setHasJob(hasJob);
            userRepository.save(user);
            return true;
        }
        return false;
    }
    
    /**
     * Get all users with a specific hasJob status
     * @param hasJob The hasJob value to filter by (true, false, or null)
     * @return List of users matching the hasJob status
     */
    public List<UserEntity> getUsersByJobStatus(Boolean hasJob) {
        if (hasJob == null) {
            // Handle the case where we want users with null hasJob status
            return userRepository.findByHasJobIsNull();
        } else {
            return userRepository.findByHasJob(hasJob);
        }
    }
    
    public UserEntity createUser(String email, String password, String firstName, String lastName, boolean isApplicant, String companyName) {
        UserEntity user = new UserEntity();
        user.setUsername(email);
        user.setPassword(hashPassword(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        
        // If isApplicant is true, then isRecruiter should be false, and vice versa
        user.setRecruiter(!isApplicant);
        
        // Only set company name if user is a recruiter (isApplicant is false)
        if (!isApplicant) {
            user.setCompanyName(companyName);
            // For recruiters, hasJob can default to null or false initially
            user.setHasJob(false);
        } else {
            // For applicants, hasJob defaults to null
            user.setHasJob(null);
        }
        
        return userRepository.save(user);
    }

}
