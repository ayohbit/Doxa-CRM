package com.doxa.crm.service;

import com.doxa.crm.domain.entity.User;
import com.doxa.crm.dto.user.TeamUserResponse;
import com.doxa.crm.repository.UserRepository;
import com.doxa.crm.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TeamUserResponse> listTeam(AuthUser user) {
        return userRepository.findByLicenseId(user.getLicenseId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private TeamUserResponse toResponse(User user) {
        return new TeamUserResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
