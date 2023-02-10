package com.solvd.laba.sinder.service.impl;

import com.solvd.laba.sinder.domain.exception.InvalidPasswordException;
import com.solvd.laba.sinder.domain.user.AuthEntity;
import com.solvd.laba.sinder.domain.user.User;
import com.solvd.laba.sinder.service.AuthenticationService;
import com.solvd.laba.sinder.service.UserService;
import com.solvd.laba.sinder.web.security.manager.JwtManager;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtManager accessJwtManager;
    private final JwtManager refreshJwtManager;
    private final JwtManager enableJwtManager;
    private final JwtManager passwordRefreshJwtManager;
    private final AuthenticationManager authenticationManager;
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String username;

    @Override
    @Transactional
    public void register(AuthEntity authEntity) {
        User user = User.builder()
                .name(authEntity.getName())
                .surname(authEntity.getSurname())
                .email(authEntity.getEmail())
                .password(passwordEncoder.encode(authEntity.getPassword()))
                .build();
        user = userService.create(user);
        String enableJwt = enableJwtManager.generateToken(user);
        // todo send email
    }

    @Override
    @Transactional(readOnly = true)
    public AuthEntity login(AuthEntity authEntity) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authEntity.getEmail(),
                        authEntity.getPassword()
                )
        );
        User user = userService.retrieveByEmail(authEntity.getEmail());
        String accessJwt = accessJwtManager.generateToken(user);
        String refreshJwt = refreshJwtManager.generateToken(user);
        return AuthEntity.builder()
                .accessToken(accessJwt)
                .refreshToken(refreshJwt)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthEntity refresh(AuthEntity authEntity) {
        String email = refreshJwtManager.extractClaim(authEntity.getRefreshToken(), Claims::getSubject);
        User user = userService.retrieveByEmail(email);
        String accessJwt = accessJwtManager.generateToken(user);
        String refreshJwt = refreshJwtManager.generateToken(user);
        return AuthEntity.builder()
                .accessToken(accessJwt)
                .refreshToken(refreshJwt)
                .build();
    }

    @Override
    @Transactional
    public AuthEntity enable(AuthEntity authEntity) {
        String email = enableJwtManager.extractClaim(authEntity.getEnableToken(), Claims::getSubject);
        User user = userService.enable(email);
        String accessJwt = accessJwtManager.generateToken(user);
        String refreshJwt = refreshJwtManager.generateToken(user);
        return AuthEntity.builder()
                .accessToken(accessJwt)
                .refreshToken(refreshJwt)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void requestPasswordRefresh(Long userId) {
        User user = userService.retrieveById(userId);
        String refreshPasswordJwt = passwordRefreshJwtManager.generateToken(user);
        // todo send email
    }

    @Override
    @Transactional
    public AuthEntity refreshPassword(AuthEntity authEntity) {
        String email = refreshJwtManager.extractClaim(authEntity.getPasswordRefreshToken(), Claims::getSubject);
        User user = userService.retrieveByEmail(email);
        user = userService.updatePassword(user, authEntity.getNewPassword());
        String accessJwt = accessJwtManager.generateToken(user);
        String refreshJwt = refreshJwtManager.generateToken(user);
        return AuthEntity.builder()
                .accessToken(accessJwt)
                .refreshToken(refreshJwt)
                .build();
    }

    @Override
    @Transactional
    public AuthEntity updatePassword(Long userId, AuthEntity authEntity) {
        User user = userService.retrieveById(userId);
        if(!BCrypt.checkpw(authEntity.getPassword(), user.getPassword())){
            throw new InvalidPasswordException("Invalid password!");
        }
        user = userService.updatePassword(user, authEntity.getNewPassword());
        String accessJwt = accessJwtManager.generateToken(user);
        String refreshJwt = refreshJwtManager.generateToken(user);
        return AuthEntity.builder()
                .accessToken(accessJwt)
                .refreshToken(refreshJwt)
                .build();
    }

    private void sendMessage(String receiver, String subject, String message){
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(username);
        mailMessage.setTo(receiver);
        mailMessage.setSubject(subject);
        mailMessage.setText(message); //template's text
        mailSender.send(mailMessage);
    }

}
