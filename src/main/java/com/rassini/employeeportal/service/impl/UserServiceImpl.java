package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.UpdateStatusRequest;
import com.rassini.employeeportal.dto.request.UserRequest;
import com.rassini.employeeportal.dto.request.UserUpdateRequest;
import com.rassini.employeeportal.dto.response.RoleResponse;
import com.rassini.employeeportal.dto.response.UserResponse;
import com.rassini.employeeportal.entity.RoleEntity;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.RoleMapper;
import com.rassini.employeeportal.mapper.UserMapper;
import com.rassini.employeeportal.repository.RoleRepository;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementación de {@link UserService}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        UserEntity user = findUserOrThrow(id);
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("El nombre de usuario '" + request.getUsername() + "' ya está en uso");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("El email '" + request.getEmail() + "' ya está en uso");
        }

        UserEntity entity = userMapper.toEntity(request);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        UserEntity saved = userRepository.save(entity);
        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        UserEntity user = findUserOrThrow(id);

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BusinessException("El nombre de usuario '" + request.getUsername() + "' ya está en uso");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("El email '" + request.getEmail() + "' ya está en uso");
            }
            user.setEmail(request.getEmail());
        }

        user.setUpdatedAt(LocalDateTime.now());
        UserEntity saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse updateStatus(Long id, UpdateStatusRequest request) {
        UserEntity user = findUserOrThrow(id);
        user.setEnabled(request.getEnabled());
        user.setUpdatedAt(LocalDateTime.now());

        UserEntity saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getUserRoles(Long id) {
        UserEntity user = findUserOrThrow(id);
        return user.getRoles().stream()
                .map(roleMapper::toResponseShallow)
                .toList();
    }

    @Override
    public UserResponse replaceUserRoles(Long id, Set<Long> roleIds) {
        UserEntity user = findUserOrThrow(id);

        List<RoleEntity> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException("Uno o más IDs de rol no existen");
        }

        user.setRoles(new HashSet<>(roles));
        user.setUpdatedAt(LocalDateTime.now());

        UserEntity saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    public com.rassini.employeeportal.dto.response.SimpleMessageResponse changePassword(Long id, com.rassini.employeeportal.dto.request.ChangePasswordRequest request) {
        UserEntity user = findUserOrThrow(id);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("La nueva contraseña y la confirmación no coinciden");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("La nueva contraseña no puede ser igual a la actual");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("La contraseña actual es incorrecta");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return new com.rassini.employeeportal.dto.response.SimpleMessageResponse(200, "Contraseña actualizada exitosamente");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private UserEntity findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }
}
