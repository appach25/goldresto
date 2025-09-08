package com.goldresto.controller;

import com.goldresto.service.PaiementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequestMapping("/simple-payments")
@PreAuthorize("hasAuthority('ROLE_OWNER')")
public class SimplePaymentController {

    @Autowired
    private PaiementService paiementService;

    @GetMapping("/by-user")
    public String showSimpleUserPayments(Model model) {
        model.addAttribute("userPayments", paiementService.getUserPaymentsGroup(null, null));
        return "payments/simple-by-user";
    }
}
