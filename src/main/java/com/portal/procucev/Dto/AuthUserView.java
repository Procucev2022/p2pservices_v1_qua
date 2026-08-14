package com.portal.procucev.Dto;

/**
 * Narrow read-only projection of the fields the authentication path actually needs.
 *
 * <p>The authentication filter runs on every request. Loading the full {@code User} entity there
 * cost three SELECTs (the row itself plus the eagerly mapped {@code role} and
 * {@code clientStatus} associations) and the entity was then discarded, because only the
 * username, password and phone are used to build the Spring Security principal.
 *
 * <p>Projecting instead of loading the entity keeps that to a single SELECT without changing any
 * entity fetch type, so nothing else in the application is affected.
 */
public interface AuthUserView {

    /**
     * Returns the account username.
     *
     * @return the username
     */
    String getUsername();

    /**
     * Returns the stored credential.
     *
     * @return the password
     */
    String getPassword();

    /**
     * Returns the account phone number.
     *
     * @return the phone number
     */
    String getPhone();
}
