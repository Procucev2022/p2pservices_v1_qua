
package com.portal.procucev.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.portal.procucev.customexception.AppException;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.dao.EmailUserRepo;
import com.portal.procucev.model.EmailUser;
import com.portal.procucev.model.ResetPassword;
import com.portal.procucev.model.User;
import com.portal.procucev.service.UserService;
import com.portal.procucev.utils.ApplicationConstants;

@RestController
@RequestMapping("/rest/users")
@CrossOrigin
public class UserController {

	@Autowired
	private UserService userServices;

	@Autowired
	EmailUserRepo emailUserRepo;

	@RequestMapping(value = "/user", method = RequestMethod.GET)
	public List<User> listUser() {
		return userServices.findAll();
	}

	@RequestMapping(value = "/user", method = RequestMethod.POST)
	public User create(@RequestBody User user) {
		return userServices.save(user);
	}

	@RequestMapping(value = "/user/{id}", method = RequestMethod.DELETE)
	public String delete(@PathVariable(value = "id") Long id) {
		userServices.delete(id);
		return "success";
	}

	@PostMapping(value = "/user/loggedUser")
	public User getUserDetails(@RequestBody User user) {

		return userServices.getUserByEmail(user);

	}

	@PostMapping(value = "/changePswd")
	public ResponseEntity<?> changePswd(@RequestBody ResetPassword reset) {
		boolean status = userServices.changePassword(reset);
		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = status ? String.format(ApplicationConstants.PASSWORD_CHANGED_SUCCESS, "")
				: String.format(ApplicationConstants.PASSWORD_CHANGED_UNSUCCESS, "");
		AppException response = new AppException(statusCode, msg, null, null);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@PostMapping(value = "/saveAuth")
	public ResponseEntity<?> saveEmailUser(@RequestBody EmailUser user) {
		try {
			EmailUser emailResponse = emailUserRepo.findByEmail(user.getEmail());
			String msg;
			if (emailResponse != null) {
				String statusCode = String.valueOf(ApplicationConstants.FAILURE);
				msg = "Already Authenticated With Email";
				throw new AppException(statusCode, msg, null, null);
			} else {
				boolean status = userServices.saveEmailuser(user);
				String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
						: String.valueOf(ApplicationConstants.FAILURE);
				msg = status ? String.format(ApplicationConstants.AUTHENTICATE_SUCCESS, "")
						: String.format(ApplicationConstants.AUTHENTICATE_UNSUCCESS, "");
				return ResponseEntity.ok(new MessageResponse(statusCode, msg));
			}
		} catch (AppException e) {
			AppException response = new AppException("200", "Already Authenticated With Email", null, null);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new AppException("Internal Server Error"));
		}
	}

	@PostMapping(value = "/updateAuth")
	public ResponseEntity<?> updateEmailUserPswd(@RequestBody EmailUser user) {
		boolean status = userServices.updateEmailUserPswd(user);
		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = status ? String.format(ApplicationConstants.PASSWORD_CHANGED_SUCCESS, "")
				: String.format(ApplicationConstants.PASSWORD_CHANGED_UNSUCCESS, "");
		AppException response = new AppException(statusCode, msg, null, null);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
	
	@PostMapping(value = "/disableUser")
	public ResponseEntity<?> disableUser(@RequestBody User user) {
		boolean status = userServices.disableUser(user);
		String statusCode = status ? String.valueOf(ApplicationConstants.SUCCESS)
				: String.valueOf(ApplicationConstants.FAILURE);
		String msg = status ? String.format(ApplicationConstants.USER_DELETE_SUCCESS, "")
				: String.format(ApplicationConstants.USER_DELETE_UNSUCCESS, "");
		AppException response = new AppException(statusCode, msg, null, null);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
}
