package io.almostrealism.auth;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A simple parent class for {@link HttpServlet}s that provide authentication via {@link AuthenticatableFactory}.
 * The {@link #doGet(HttpServletRequest, HttpServletResponse)} method of this class will check that the password
 * matches and return either {@link HttpServletResponse#SC_OK} or {@link HttpServletResponse#SC_FORBIDDEN}.
 */
public abstract class AuthenticationServlet<T extends Authenticatable> extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private AuthenticatableFactory<T> factory;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    protected AuthenticationServlet(AuthenticatableFactory<T> factory) {
    		this.factory = factory;
    	}
    
	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		this.factory.init();
	}
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String identifier = request.getParameter("identifier");
		String password = request.getParameter("password");
		
		if (factory.getAuthenticatable(identifier).getPassword().equals(password)) {
			response.getWriter().append("OK");
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.getWriter().append("FORBIDDEN");
		}
	}
}
