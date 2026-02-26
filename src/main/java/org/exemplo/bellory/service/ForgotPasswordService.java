package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.auth.MaskedEmailDTO;
import org.exemplo.bellory.model.dto.auth.ResetTokenDTO;
import org.exemplo.bellory.model.entity.email.EmailTemplate;
import org.exemplo.bellory.model.entity.users.PasswordResetCode;
import org.exemplo.bellory.model.entity.users.User;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.users.AdminRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.exemplo.bellory.model.repository.users.PasswordResetCodeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordService {

    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final AdminRepository adminRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ClienteRepository clienteRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int CODE_EXPIRATION_MINUTES = 10;
    private static final int TOKEN_EXPIRATION_MINUTES = 15;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public MaskedEmailDTO requestPasswordReset(String identifier) {
        User user = findUserByIdentifier(identifier.trim().toLowerCase());
        if (user == null) {
            throw new RuntimeException("Usuário não encontrado");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new RuntimeException("Usuário não possui e-mail cadastrado. Entre em contato com o administrador.");
        }

        String userRole = user.getRole();

        // Invalidar codigos anteriores
        passwordResetCodeRepository.invalidateAllForUser(user.getId(), userRole);

        // Gerar codigo de 6 digitos
        String code = generateCode();

        // Salvar no banco
        PasswordResetCode resetCode = PasswordResetCode.builder()
                .userId(user.getId())
                .userRole(userRole)
                .email(user.getEmail())
                .code(code)
                .codeExpiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES))
                .build();

        passwordResetCodeRepository.save(resetCode);

        // Enviar email
        Map<String, Object> variables = new HashMap<>();
        variables.put("nomeCompleto", user.getNomeCompleto());
        variables.put("codigo", code);
        variables.put("validadeMinutos", CODE_EXPIRATION_MINUTES);

        emailService.enviarEmailComTemplate(
                List.of(user.getEmail()),
                EmailTemplate.RESETAR_SENHA,
                variables
        );

        log.info("Código de recuperação enviado para o usuário: {} (role: {})", user.getUsername(), userRole);

        return new MaskedEmailDTO(maskEmail(user.getEmail()));
    }

    @Transactional
    public ResetTokenDTO verifyCode(String identifier, String code) {
        User user = findUserByIdentifier(identifier.trim().toLowerCase());
        if (user == null) {
            throw new RuntimeException("Usuário não encontrado");
        }

        String userRole = user.getRole();

        Optional<PasswordResetCode> resetCodeOpt = passwordResetCodeRepository
                .findValidCode(user.getId(), userRole, code);

        if (resetCodeOpt.isEmpty()) {
            throw new RuntimeException("Código inválido ou expirado");
        }

        PasswordResetCode resetCode = resetCodeOpt.get();

        // Marcar codigo como usado
        resetCode.setCodeUsed(true);

        // Gerar reset token
        String resetToken = UUID.randomUUID().toString();
        resetCode.setResetToken(resetToken);
        resetCode.setTokenExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES));

        passwordResetCodeRepository.save(resetCode);

        log.info("Código verificado com sucesso para o usuário: {} (role: {})", user.getUsername(), userRole);

        return new ResetTokenDTO(resetToken);
    }

    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        Optional<PasswordResetCode> resetCodeOpt = passwordResetCodeRepository
                .findValidResetToken(resetToken);

        if (resetCodeOpt.isEmpty()) {
            throw new RuntimeException("Token inválido ou expirado");
        }

        PasswordResetCode resetCode = resetCodeOpt.get();

        String encodedPassword = passwordEncoder.encode(newPassword);

        // Atualizar senha no repositorio correto
        updateUserPassword(resetCode.getUserId(), resetCode.getUserRole(), encodedPassword);

        // Invalidar todos os codes/tokens do usuario
        passwordResetCodeRepository.invalidateAllForUser(resetCode.getUserId(), resetCode.getUserRole());

        log.info("Senha redefinida com sucesso para userId: {} (role: {})", resetCode.getUserId(), resetCode.getUserRole());
    }

    private User findUserByIdentifier(String identifier) {
        // Buscar por username (prioridade: Admin > Funcionario > Cliente)
        Optional<? extends User> user = adminRepository.findByUsername(identifier);
        if (user.isPresent()) return user.get();

        user = funcionarioRepository.findByUsername(identifier);
        if (user.isPresent()) return user.get();

        user = clienteRepository.findByUsername(identifier);
        if (user.isPresent()) return user.get();

        // Buscar por email (prioridade: Admin > Funcionario > Cliente)
        var admins = adminRepository.findAllByEmailIgnoreCase(identifier);
        if (!admins.isEmpty()) return admins.get(0);

        var funcionarios = funcionarioRepository.findAllByEmailIgnoreCase(identifier);
        if (!funcionarios.isEmpty()) return funcionarios.get(0);

        var clientes = clienteRepository.findAllByEmailIgnoreCase(identifier);
        if (!clientes.isEmpty()) return clientes.get(0);

        return null;
    }

    private void updateUserPassword(Long userId, String userRole, String encodedPassword) {
        switch (userRole) {
            case "ROLE_SUPERADMIN" -> {
                var admin = adminRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Admin não encontrado"));
                admin.setPassword(encodedPassword);
                adminRepository.save(admin);
            }
            case "ROLE_FUNCIONARIO", "ROLE_ADMIN", "ROLE_GERENTE", "ROLE_RECEPCAO" -> {
                var funcionario = funcionarioRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));
                funcionario.setPassword(encodedPassword);
                funcionarioRepository.save(funcionario);
            }
            case "ROLE_CLIENTE" -> {
                var cliente = clienteRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
                cliente.setPassword(encodedPassword);
                clienteRepository.save(cliente);
            }
            default -> throw new RuntimeException("Tipo de usuário desconhecido: " + userRole);
        }
    }

    private String generateCode() {
        int code = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email.charAt(0) + "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
