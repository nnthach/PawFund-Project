package org.demo.huyminh.Service.Impl;

import com.nimbusds.jwt.SignedJWT;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.huyminh.DTO.Reponse.BriefVolunteerResponse;
import org.demo.huyminh.DTO.Reponse.UserResponse;
import org.demo.huyminh.DTO.Request.ChangePasswordRequest;
import org.demo.huyminh.DTO.Request.PasswordCreationRequest;
import org.demo.huyminh.DTO.Request.UserUpdateRequest;
import org.demo.huyminh.Entity.Role;
import org.demo.huyminh.Entity.User;
import org.demo.huyminh.Exception.AppException;
import org.demo.huyminh.Exception.ErrorCode;
import org.demo.huyminh.Mapper.UserMapper;
import org.demo.huyminh.Repository.RoleRepository;
import org.demo.huyminh.Repository.UserRepository;
import org.demo.huyminh.Service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * @author Minh
 * Date: 9/24/2024
 * Time: 7:34 AM
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public List<UserResponse> getUsers() {
        log.info("In method get Users");
        return userRepository.findAll().stream()
                .map(userMapper::toUserResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN') || hasRole('VOLUNTEER')")
    @Override
    public List<BriefVolunteerResponse> getVolunteers() {

        return userRepository.findUsersByRole(Role.builder().name("VOLUNTEER").build()).stream()
                .map(user -> BriefVolunteerResponse.builder()
                        .username(user.getUsername())
                        .firstname(user.getFirstname())
                        .lastname(user.getLastname()).build()).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public List<UserResponse> getUsersByRole(String role, String sort, String sortBy, String keyword) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> userRoot = cq.from(User.class);

        Predicate predicate;

        if ("ALL".equals(role)) {
            predicate = cb.isTrue(cb.literal(true));
        } else if ("ADMIN".equals(role)) {
            predicate = cb.and(
                    cb.isMember(roleRepository.findById("ADMIN")
                            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTS)), userRoot.get("roles"))
            );
        } else {
            Role existingRole = roleRepository.findById(role.toUpperCase())
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTS));

            Predicate notAdminPredicate = cb.not(
                    cb.isMember(roleRepository.findById("ADMIN")
                            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTS)), userRoot.get("roles"))
            );

            predicate = cb.and(
                    notAdminPredicate,
                    cb.isMember(existingRole, userRoot.get("roles"))
            );
        }

        cq.where(predicate);

        if ("username".equals(sortBy)) {
            cq.orderBy("ASC".equals(sort) ? cb.asc(userRoot.get("username")) : cb.desc(userRoot.get("username")));
        } else if ("createdAt".equals(sortBy)) {
            cq.orderBy("ASC".equals(sort) ? cb.asc(userRoot.get("createdAt")) : cb.desc(userRoot.get("createdAt")));
        } else if ("applicationQuantity".equals(sortBy)) {
            cq.orderBy("ASC".equals(sort) ? cb.asc(userRoot.get("applicationQuantity")) : cb.desc(userRoot.get("applicationQuantity")));
        }

        List<UserResponse> users = new ArrayList<>();
        for (User user : entityManager.createQuery(cq).getResultList()) {
            UserResponse userResponse = userMapper.toUserResponse(user);
            users.add(userResponse);
        }
        return users;
    }

    //    @PostAuthorize("hasRole('ADMIN') || returnObject.username == authentication.name")
    @Override
    public UserResponse getUser(String id) {
        log.info("In method get User: {}", id);
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found")));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public UserResponse updateUser(String id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));

        userMapper.updateUser(user, request);

        var roles = roleRepository.findAllById(request.getRoles());
        user.setRoles(new HashSet<>(roles));

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    @Override
    public void deleteUser(String id) {

        if (!userRepository.existsById(id)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTS);
        }

        userRepository.deleteById(id);

        if (userRepository.existsById(id)) {
            log.error("Failed to delete permission: {}", id);
            throw new AppException(ErrorCode.DELETE_USER_FAILED);
        }
    }

    @Override
    public User findByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);

        if (user.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_EXISTS);
        }

        return user.get();
    }

    @Override
    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        var name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));

        var userResponse = userMapper.toUserResponse(user);
        userResponse.setNoPassword(!StringUtils.hasText(user.getPassword()));

        return userResponse;
    }

    @Transactional
    @Override
    public UserResponse createPassword(@Valid PasswordCreationRequest request) {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (StringUtils.hasText(user.getPassword()))
            throw new AppException(ErrorCode.PASSWORD_EXISTED);

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        return null;
    }

    @Override
    public User findByToken(String token) {
        String username = null;
        try {
            username = SignedJWT.parse(token).getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            throw new RuntimeException("Find user by token failed");
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));
    }

    @Override
    public void changePassword(String id, ChangePasswordRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.OLD_PASSWORD_WRONG);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.CONFIRM_PASSWORD_WRONG);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
