package io.github.androoideka.vm_manager.runners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import io.github.androoideka.vm_manager.configuration.PermUtil;
import io.github.androoideka.vm_manager.model.User;
import io.github.androoideka.vm_manager.repositories.UserRepository;

@Component
public class TestRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public TestRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User user = new User();
        user.setUsername("agasic218rn@raf.rs");
        user.setName("Andrej");
        user.setSurname("Gasic");
        user.setPassword(this.passwordEncoder.encode("tianming"));
        user.setAuthorities(PermUtil.AUTHORITIES);
        this.userRepository.save(user);
    }
}
