package com.quickapp.service;

import com.quickapp.data.entities.UserEntity;
import com.quickapp.data.repositories.UserRepository;
import com.quickapp.shared.Authority;
import com.quickapp.shared.UserDto;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Environment environment;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public UserServiceImpl(Environment environment, UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.environment = environment;
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    public UserDto createUser(UserDto userDetails) {
        // TODO Auto-generated method stub

        userDetails.setEncryptedPassword(bCryptPasswordEncoder.encode(userDetails.getPassword()));

        UserEntity storedUser = new UserEntity();

        BeanUtils.copyProperties(userDetails, storedUser);

        if (storedUser.getAuthorities() == null) {
            List<Authority> authoritiesCostumer = new ArrayList<>();
            authoritiesCostumer.add(Authority.COSTUMER);

            storedUser.setAuthorities(authoritiesCostumer);
        }

        storedUser = userRepository.save(storedUser);

        UserDto returnValue = new UserDto();

        BeanUtils.copyProperties(storedUser, returnValue);

        return returnValue;
    }

    @Override
    public UserDto getUserByEmail(String email) {
        Optional<UserEntity> userEntity = userRepository.findByEmail(email);
        if (!userEntity.isPresent()) return null;
        UserDto returnValue = new UserDto();
        BeanUtils.copyProperties(userEntity.get(), returnValue);
        return returnValue;
    }

    @Override
    public UserDto getUserByUserId(String userId) {
        Optional<UserEntity> userEntity = userRepository.findById(userId);
        if (!userEntity.isPresent()) return null;
        UserDto returnValue = new UserDto();
        BeanUtils.copyProperties(userEntity.get(), returnValue);
        return returnValue;
    }

    @Override
    public UserDto getUserByToken(String token) {
        token = token.replace(environment.getProperty("authorization.token.header.prefix"), "").trim();
        String userId = Jwts.parser()
                .setSigningKey(environment.getProperty("jwt.secret"))
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        return this.getUserByUserId(userId);
    }

    @Override
    public List<UserDto> getAllUsers() {
        logger.info("Calling users service");
        List<UserEntity> storedUsers = this.userRepository.findAll();
        List<UserDto> returnedValue = new ArrayList<>();
        storedUsers.forEach(user -> {
            UserDto dto = new UserDto();
            BeanUtils.copyProperties(user,dto);
            returnedValue.add(dto);
        });
        return returnedValue;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<UserEntity> userEntity = userRepository.findByEmail(email);
        if (!userEntity.isPresent()) throw new UsernameNotFoundException(email);
        return new User(userEntity.get().getEmail(), userEntity.get().getEncryptedPassword(), true, true, true, true, new ArrayList<>());
    }
}