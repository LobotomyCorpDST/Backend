package com.devsop.project.apartmentinvoice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.repository.LeaseRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/leases") // ✅ ไม่ใช่ /api
@RequiredArgsConstructor
public class LeasePrintController {

  private final LeaseRepository leaseRepo;

  @GetMapping("/{id}/print") // ✅ เส้นทางเป็น /leases/{id}/print
  public ModelAndView print(@PathVariable Long id) {
    Lease lease = leaseRepo.findByIdWithRefs(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease not found: " + id));

    ModelAndView mv = new ModelAndView("lease/print");
    mv.addObject("lease", lease);
    mv.addObject("room", lease.getRoom());
    mv.addObject("tenant", lease.getTenant());
    return mv;
  }
}
