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
import com.rassini.employeeportal.dto.response.BusinessUnitResponse;
import com.rassini.employeeportal.entity.BusinessUnitEntity;
import com.rassini.employeeportal.mapper.BusinessUnitMapper;
import com.rassini.employeeportal.repository.BusinessUnitRepository;
import com.rassini.employeeportal.repository.RoleRepository;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.UserService;
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
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final BusinessUnitMapper businessUnitMapper;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           BusinessUnitRepository businessUnitRepository, UserMapper userMapper,
                           RoleMapper roleMapper, BusinessUnitMapper businessUnitMapper,
                           org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.businessUnitRepository = businessUnitRepository;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.businessUnitMapper = businessUnitMapper;
        this.passwordEncoder = passwordEncoder;
    }

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
        entity.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        applyBusinessUnitConfiguration(
                entity,
                request.getHasAllBusinessUnits(),
                request.getBusinessUnitIds()
        );

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

        if (request.getHasAllBusinessUnits() != null || request.getBusinessUnitIds() != null) {
            Boolean hasAllBUs = request.getHasAllBusinessUnits() != null ? request.getHasAllBusinessUnits() : user.getHasAllBusinessUnits();
            Set<Long> businessUnitIds = request.getBusinessUnitIds();

            // Si no se pasaron las asignaciones de BU, mantenemos las actuales del usuario (siempre que no pase a global)
            if (businessUnitIds == null && !Boolean.TRUE.equals(hasAllBUs)) {
                businessUnitIds = new HashSet<>();
                for (BusinessUnitEntity bu : user.getBusinessUnits()) {
                    businessUnitIds.add(bu.getId());
                }
            }

            applyBusinessUnitConfiguration(user, hasAllBUs, businessUnitIds);
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

    @Override
    @Transactional(readOnly = true)
    public List<BusinessUnitResponse> getUserBusinessUnits(Long id) {
        UserEntity user = findUserOrThrow(id);
        return businessUnitMapper.toResponseList(user.getBusinessUnits());
    }

    @Override
    public UserResponse replaceUserBusinessUnits(Long id, Set<Long> businessUnitIds) {
        UserEntity user = findUserOrThrow(id);
        applyBusinessUnitConfiguration(user, user.getHasAllBusinessUnits(), businessUnitIds);
        user.setUpdatedAt(LocalDateTime.now());
        UserEntity saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    // ─── Helpers privados de Negocio y Validación ──────────────────────────────

    private void applyBusinessUnitConfiguration(
            UserEntity user,
            Boolean hasAllBusinessUnits,
            Set<Long> businessUnitIds
    ) {
        boolean global = hasAllBusinessUnits != null ? hasAllBusinessUnits : false;
        user.setHasAllBusinessUnits(global);

        if (global) {
            user.getBusinessUnits().clear();
        } else {
            if (businessUnitIds == null || businessUnitIds.isEmpty()) {
                throw new BusinessException("Al menos una Business Unit debe ser asignada cuando no se tiene acceso global");
            }

            // Buscar y validar las BUs indicadas
            List<BusinessUnitEntity> bus = businessUnitRepository.findAllById(businessUnitIds);
            if (bus.size() != businessUnitIds.size()) {
                throw new BusinessException("Una o más Business Units especificadas no existen");
            }

            // Validar que todas estén habilitadas
            for (BusinessUnitEntity bu : bus) {
                if (!Boolean.TRUE.equals(bu.getEnabled())) {
                    throw new BusinessException("La Business Unit '" + bu.getName() + "' está deshabilitada y no puede ser asignada");
                }
            }

            user.setBusinessUnits(new HashSet<>(bus));
        }
    }

    private UserEntity findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }
}

