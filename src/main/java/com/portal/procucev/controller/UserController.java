package com.portal.procucev.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.User;

@RestController
@RequestMapping("/rest/users")
@CrossOrigin
public class UserController {
	
	@Autowired
	private UserDao userDao;
	
	@RequestMapping(value = "/user", method = RequestMethod.GET)
	public List<User> listUser() {
		return userDao.findAll();
	}
}
