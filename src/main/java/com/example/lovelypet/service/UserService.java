package com.example.lovelypet.service;

import com.example.lovelypet.entity.User;
import com.example.lovelypet.exception.BaseException;
import com.example.lovelypet.exception.UserException;
import com.example.lovelypet.repository.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository repository;

    private final PasswordEncoder passwordEncoder;


    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }


    public User create(
            String userName,
            String passWord,
            String name,
            String email,
            String phoneNumber,
            String token,
            Date tokenExpireDate
    ) throws BaseException {

        //validate
        if (Objects.isNull(email)) {
            //throw error email null
            throw UserException.createEmailNull();
        }

        if (Objects.isNull(passWord)) {
            //throw error password null
            throw UserException.createPasswordNull();
        }

        if (Objects.isNull(name)) {
            //throw error name null
            throw UserException.createNameNull();
        }

        if (Objects.isNull(userName)) {
            throw UserException.createUserNameNull();
        }

        if (Objects.isNull(phoneNumber)) {
            throw UserException.createPhoneNumberNull();
        }

        //verify
        if (repository.existsByEmail(email)) {
            //throw error email duplicated
            throw UserException.createEmailDuplicated();
        }
        if (repository.existsByUserName(userName)) {
            //throw error email duplicated
            throw UserException.createUserNameDuplicated();
        }

        //save
        else {
            User entity = new User();
            entity.setUserName(userName);
            entity.setPassWord(passwordEncoder.encode(passWord));
            entity.setName(name);
            entity.setEmail(email);
            entity.setPhoneNumber(phoneNumber);
            entity.setToken(token);
            entity.setTokenExpire(tokenExpireDate);
            return repository.save(entity);
        }
    }

    @Cacheable(value = "user", key = "#idU", unless = "#result == null")
    public Optional<User> findById(int idU) throws BaseException {
        Optional<User> user = repository.findById(idU);
        return user;
    }

    public Optional<User> findByEmail(String email) throws BaseException {
        Optional<User> user = repository.findByEmail(email);
        return user;
    }

    public Optional<User> findByToken(String token) throws BaseException {
        Optional<User> user = repository.findByToken(token);
        return user;
    }


    public Optional<User> findLog(String userName) throws BaseException {
        Optional<User> user = repository.findByUserName(userName);
        return user;
    }

    public User resetPassword(int id, String newPassword) throws BaseException {
        Optional<User> opt = repository.findById(id);
        User user = opt.get();
        user.setPassWord(newPassword);
        return repository.save(user);
    }

    @CachePut(value = "user", key = "#id")
    public User update(User user) throws BaseException {
        return repository.save(user);
    }

    public User updateNormalData(int id, String name, String phoneNumber) throws BaseException {
        Optional<User> opt = repository.findById(id);
        if (opt.isEmpty()) {
            throw UserException.notFound();
        }
        User user = opt.get();
        if (!Objects.isNull(name)) {
            user.setName(name);
        }
        if (!Objects.isNull(phoneNumber)) {
            user.setPhoneNumber(phoneNumber);
        }

        return repository.save(user);
    }

    @CacheEvict(value = "user", key = "#id")
    //@CacheEvict(value = "user",allEntries = true)//ในกรณีลบทั้งหมด
    public void deleteByIdU(String idU) {
        repository.deleteById(idU);
    }

    public boolean matchPassword(String requestPass, String dataPass) {
        return passwordEncoder.matches(requestPass, dataPass);
    }
}
