package com.library.management.servlet;

import com.library.management.jdbc.JdbcReportRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Demonstrates direct Servlet-based web integration.
 *
 * The servlet obtains the application's JDBC repository from the Spring
 * application context and displays live library statistics in a browser.
 */
@WebServlet(name = "LibraryStatusServlet", urlPatterns = "/servlet/library-status")
public class LibraryStatusServlet extends HttpServlet {

    private JdbcReportRepository jdbcReportRepository;

    @Override
    public void init() throws ServletException {
        WebApplicationContext context =
                WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
        jdbcReportRepository = context.getBean(JdbcReportRepository.class);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int totalBooks = jdbcReportRepository.getTotalBooks();
        int availableBooks = jdbcReportRepository.getAvailableBooks();
        int activeMembers = jdbcReportRepository.getActiveMembers();

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html><head><title>Library Status</title>");
            out.println("<style>");
            out.println("body{font-family:Arial,sans-serif;padding:40px;background:#f6f7fb;color:#182230;}");
            out.println(".card{max-width:650px;margin:auto;background:white;padding:30px;border-radius:14px;box-shadow:0 8px 30px rgba(0,0,0,.08);}");
            out.println("h1{margin-top:0;color:#5b4ce8;} .item{padding:12px 0;border-bottom:1px solid #eee;}");
            out.println("a{color:#5b4ce8;font-weight:bold;text-decoration:none;}");
            out.println("</style></head><body>");
            out.println("<div class='card'>");
            out.println("<h1>LibraDesk Library Status</h1>");
            out.println("<p>This page is served by a Java HttpServlet and reads live data through JDBC.</p>");
            out.println("<div class='item'><b>Total Books:</b> " + totalBooks + "</div>");
            out.println("<div class='item'><b>Available Books:</b> " + availableBooks + "</div>");
            out.println("<div class='item'><b>Active Members:</b> " + activeMembers + "</div>");
            out.println("<p><a href='/'>Back to LibraDesk</a></p>");
            out.println("</div></body></html>");
        }
    }
}
