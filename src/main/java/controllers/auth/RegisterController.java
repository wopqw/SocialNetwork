package controllers.auth;

import common.BaseServlet;
import lombok.extern.slf4j.Slf4j;
import model.Role;
import model.User;
import model.UserRole;
import security.StringEncryptUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by wopqw on 01.11.16.
 */
@Slf4j
@WebServlet(urlPatterns = {"/register"})
public class RegisterController extends BaseServlet {

    // TODO: 01.11.16 Валидация на jsp и тут


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        log.info("doGet in registerController");
        req.getRequestDispatcher("/WEB-INF/register.jsp").forward(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession httpSession = req.getSession();

        User.UserBuilder userBuilder = User.builder();

        Map<String, String[]> reqParameterMap = req.getParameterMap();

        String username = reqParameterMap.get("j_username")[0];
        String hash = "";

        try {
            hash = StringEncryptUtil.encrypt(reqParameterMap.get("j_password")[0]);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        userDAO.addUser(userBuilder
                .id(0L)
                .username(username)
                .password(hash)
                .email(reqParameterMap.get("email")[0])
                .firstName(reqParameterMap.get("firstName")[0])
                .lastName(reqParameterMap.get("lastName")[0])
                .build()
        );

        UserRole userRole = new UserRole(username, Role.USER.toString());
        userRoleDAO.addUserRole(userRole);

        Optional<User> optUser = userDAO.getByUsername(username);

        if(optUser.isPresent()){
            User user = optUser.get();
            log.info("user is "+user.toString());
            httpSession.setAttribute(USER,user);
            log.info("method: "+req.getMethod());
            httpSession.setAttribute(USER_ROLE, new HashSet<>(Collections.singletonList(userRole)));
//            req.getRequestDispatcher("/home/").forward(req,resp);
            String url =req.getServerName()+":"+req.getServerPort()+"/home";
            resp.sendRedirect(url);

        } else {
            req.getRequestDispatcher("/WEB-INF/error.jsp").forward(req,resp);
        }
    }
}
