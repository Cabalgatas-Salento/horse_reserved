package horse_reserved.service;

import horse_reserved.dto.OAuth2UserInfo;
import horse_reserved.exception.OAuth2AuthenticationProcessingException;
import horse_reserved.model.Rol;
import horse_reserved.model.TipoDocumento;
import horse_reserved.model.Usuario;
import horse_reserved.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Metodo para cargar la informacion del usuario al iniciar sesion con google
     * @param userRequest
     * @return
     * @throws OAuth2AuthenticationException
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OAuth2AuthenticationProcessingException(ex.getMessage(), ex);
        }
    }

    /**
     *Metodo que define si se registra o se inicia sesion al iniciar sesion con google
     * @param userRequest
     * @param oAuth2User
     * @return
     */
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo oAuth2UserInfo;
        if ("google".equals(registrationId)) {
            oAuth2UserInfo = OAuth2UserInfo.fromGoogle(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationProcessingException(
                    "Login con " + registrationId + " no está soportado");
        }

        if (oAuth2UserInfo.getEmail() == null || oAuth2UserInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationProcessingException("Email no encontrado del proveedor OAuth2");
        }

        Optional<Usuario> usuarioOptional = usuarioRepository.findByEmail(oAuth2UserInfo.getEmail());
        Usuario usuario;

        if (usuarioOptional.isPresent()) {
            usuario = usuarioOptional.get();
            // Actualizar información si es necesario
            usuario = updateExistingUser(usuario, oAuth2UserInfo);
        } else {
            usuario = registerNewUser(oAuth2UserInfo);
        }

        return oAuth2User;
    }

    /**
     * Metodo para extraer los apellidos de una cuenta de Google
     * @param nombreCompleto
     * @return
     */
    private String[] extraerApellidos(String nombreCompleto) {
        String[] partes = nombreCompleto.trim().split("\\s+");

        String primerApellido = "";
        String segundoApellido = "";

        if (partes.length == 2) {
            // "Juan García" → un apellido
            primerApellido = partes[1];
        } else if (partes.length >= 3) {
            // "Juan García López" o "Juan Carlos García López" → últimas dos partes
            primerApellido = partes[partes.length - 2];
            segundoApellido = partes[partes.length - 1];
        }

        return new String[]{primerApellido, segundoApellido};
    }

    /**
     * Metodo para registrar un usuario despues de iniciar sesion con google
     * @param oAuth2UserInfo
     * @return
     */
    private Usuario registerNewUser(OAuth2UserInfo oAuth2UserInfo) {
        // Extraer apellidos del nombre completo
        String[] apellidos = extraerApellidos(oAuth2UserInfo.getName());
        String primerNombre = oAuth2UserInfo.getName().trim().split("\\s+")[0];
        String primerApellido = apellidos[0];
        //String segundoApellido = apellidos[1];

        //Prueba(DESPUES BORRAR)
        /*
        Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);
        logger.info("Nombre completo OAuth2: {}", oAuth2UserInfo.getName());
        logger.info("Primer nombre: {}", primerNombre);
        logger.info("Primer apellido: {}", primerApellido);
        logger.info("Segundo apellido: {}", segundoApellido);
        */

        Usuario usuario = Usuario.builder()
                .primerNombre(primerNombre)
                .primerApellido(primerApellido)
                .email(oAuth2UserInfo.getEmail())
                .passwordHash(null) // No hay contraseña para usuarios OAuth2
                .tipoDocumento(TipoDocumento.CEDULA) // Por defecto, despues se debe cambiar
                .documento("OAUTH2-" + oAuth2UserInfo.getId()) // Temporal
                .role(Rol.CLIENTE)
                .isActive(true)
                .build();

        return usuarioRepository.save(usuario);
    }

    /**
     * metodo para actualizar la informacion de un usuario despues de iniciar sesion
     * con google de ser necesario
     * @param usuario
     * @param oAuth2UserInfo
     * @return
     */
    private Usuario updateExistingUser(Usuario usuario, OAuth2UserInfo oAuth2UserInfo) {
        // Actualizar información si es necesario
        String[] apellidos = extraerApellidos(oAuth2UserInfo.getName());
        String primerNombre = oAuth2UserInfo.getName().trim().split("\\s+")[0];

        usuario.setPrimerNombre(primerNombre);
        usuario.setPrimerApellido(apellidos[0]);
        //usuario.setSegundoApellido(apellidos[1].isEmpty() ? null : apellidos[1]);

        return usuarioRepository.save(usuario);
    }
}