package com.example.demo.controller.command;

import com.example.demo.dao.model.Order;
import com.example.demo.dao.model.User;
import com.example.demo.service.OrderService;
import com.example.demo.service.impl.OrderServiceImpl;
import com.example.demo.service.impl.UserServiceImpl;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class LoginCommand implements BaseCommand {

    private static final Logger logger = LogManager.getLogger(LoginCommand.class);
    private final UserService userService = new UserServiceImpl();
    private final OrderService orderService = new OrderServiceImpl();

    @Override
    public String execute(HttpServletRequest request) {
        if (!validateLoginForm(request)) {
            logger.warn("Login or password is missing in the form.");
            request.setAttribute("errorLoginPassMessage", "Please enter both login and password.");
            return "/jsp/login.jsp";
        }

        String login = request.getParameter("username");
        String password = request.getParameter("password");

        String page;
        if (userService.authenticate(login, password)) {
            logger.info("Authentication successful for user: {}", login);
            User user = userService.getUserByUsername(login);
            if (user != null) {
                request.getSession().setAttribute("userId", user.getUserId());
                request.getSession().setAttribute("user_name", login);
                request.getSession().setAttribute("role", user.getRole());

                if ("admin".equals(user.getRole())) {
                    List<Order> orders = orderService.getAllOrders();
                    request.setAttribute("orders", orders);
                    page = "/jsp/orderHistory.jsp";
                    logger.info("Admin user {} accessed the order history page.", login);
                } else {
                    page = "/jsp/main.jsp";
                    logger.info("User {} logged in and redirected to the main page.", login);
                }
            } else {
                logger.warn("User {} not found in the database.", login);
                request.setAttribute("errorLoginPassMessage", "User not found.");
                page = "/jsp/login.jsp";
            }
        } else {
            logger.warn("Failed authentication attempt for user: {}", login);
            request.setAttribute("errorLoginPassMessage", "Incorrect login or password");
            page = "/jsp/login.jsp";
        }
        return page;
    }

    private boolean validateLoginForm(HttpServletRequest request) {
        String login = request.getParameter("username");
        String password = request.getParameter("password");
        return login != null && !login.isEmpty() && password != null && !password.isEmpty();
    }
}
