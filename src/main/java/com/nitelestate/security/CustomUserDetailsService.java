package com.nitelestate.security;

import com.nitelestate.user.PhoneNumbers;
import com.nitelestate.user.User;
import com.nitelestate.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * {@code identifier} is either an email or a phone number (see LoginRequest) - tried as
     * email first since that's unambiguous, falling back to a normalized phone match.
     */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByEmailIgnoreCase(identifier);
        if (user.isEmpty()) {
            String normalizedPhone = PhoneNumbers.normalize(identifier);
            if (normalizedPhone != null) {
                user = userRepository.findByPhone(normalizedPhone);
            }
        }
        return user.map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email or phone " + identifier));
    }
}
