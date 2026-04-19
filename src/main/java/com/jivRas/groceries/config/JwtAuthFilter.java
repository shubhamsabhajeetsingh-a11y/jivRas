package com.jivRas.groceries.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException, java.io.IOException {

		String authHeader = request.getHeader("Authorization");
		String token = null;
		String username = null;

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7);
			username = jwtService.extractUsername(token);
		}

		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

			UserDetails userDetails = userDetailsService.loadUserByUsername(username);

			if (jwtService.validateToken(token, userDetails)) {

				// Extract role from JWT claim directly
				String roleFromToken = jwtService.extractClaim(
						token,
						claims -> claims.get("role", String.class)
				);

				// Cross-check: JWT role must match DB role
				String dbRole = userDetails.getAuthorities()
						.stream()
						.map(a -> a.getAuthority())
						.findFirst()
						.orElse(null);

				if (roleFromToken != null
						&& !roleFromToken.equals(dbRole)
						&& !("ROLE_" + roleFromToken).equals(dbRole)) {
					// Role mismatch between JWT and DB
					// Reject the request
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.getWriter().write(
							"{\"error\":\"Token role mismatch\"}"
					);
					return; // stop filter chain
				}

				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
						null, userDetails.getAuthorities());

				SecurityContextHolder.getContext().setAuthentication(authToken);
			}
		}

		// No valid JWT → treat the caller as GUEST so the RBAC aspect can enforce
		// guest-level permissions instead of leaking a 500 NullPointerException.
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			UsernamePasswordAuthenticationToken guestAuth = new UsernamePasswordAuthenticationToken(
					"guest", null,
					List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
			SecurityContextHolder.getContext().setAuthentication(guestAuth);
		}

		filterChain.doFilter(request, response);
	}
}