package com.coursehub.service;

import com.coursehub.dto.request.UpdateProfileRequest;
import com.coursehub.dto.response.UserResponse;
import com.coursehub.entity.User;
import com.coursehub.exception.ConflictException;
import com.coursehub.mapper.UserMapper;
import com.coursehub.repository.UserRepository;
import com.coursehub.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        return UserMapper.toResponse(currentUserService.getCurrentUser());
    }

    @Transactional
    public UserResponse updateMyProfile(UpdateProfileRequest request) {
        User user = currentUserService.getCurrentUser();

        String phone = (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty())
                ? null : request.getPhoneNumber();
        if (phone != null && userRepository.existsByPhoneNumberAndIdNot(phone, user.getId())) {
            throw new ConflictException("Phone number is already registered");
        }

        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(phone);
        return UserMapper.toResponse(userRepository.save(user));
    }
}
