package uk.co.visad.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.visad.entity.InvoiceHistory;
import uk.co.visad.repository.InvoiceHistoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final uk.co.visad.repository.TravelerRepository travelerRepository;
    private final uk.co.visad.repository.DependentRepository dependentRepository;
    private final uk.co.visad.repository.InvoiceHistoryRepository invoiceHistoryRepository;

    @Transactional(readOnly = true)
    public List<InvoiceHistory> getHistory(Long travelerId) {
        return invoiceHistoryRepository.findByRecordIdOrderBySentAtDesc(travelerId);
    }

    @Transactional(readOnly = true)
    public uk.co.visad.dto.InvoiceViewDto generateInvoiceView(String recordType, Long id) {
        if (!"traveler".equalsIgnoreCase(recordType)) {
             // For now, minimal support if recordType is dependent (usually invoices are for main traveler)
             // But following PHP logic, we fetch 'dependents' table if type is dependent
             // Let's stick to main traveler logic primarily as PHP implies structure.
             // If ID is dependent, we treat it as main record if direct link (PHP line 26)
             // But for full structure we need the traveler entity.
             // Let's implement for 'traveler' first as primary use case.
        }

        uk.co.visad.entity.Traveler traveler = travelerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Traveler not found"));

        // 1. Customer Info
        String customerName = getFullName(traveler.getFirstName(), traveler.getLastName());
        if (customerName.isEmpty()) customerName = traveler.getName();
        if (customerName == null) customerName = "Customer";

        List<String> addressLines = new java.util.ArrayList<>();
        if (hasText(traveler.getAddressLine1())) addressLines.add(traveler.getAddressLine1());
        if (hasText(traveler.getAddressLine2())) addressLines.add(traveler.getAddressLine2());
        String cityState = join(", ", traveler.getCity(), traveler.getStateProvince());
        if (!cityState.isEmpty()) addressLines.add(cityState);
        String zipCountry = join(", ", traveler.getZipCode(), traveler.getCountry());
        if (!zipCountry.isEmpty()) addressLines.add(zipCountry);

        // 2. Invoice Number / Dates
        String invoiceNumber = "INV-" + String.format("%04d", traveler.getId());
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy");
        
        // 3. Pricing Logic (Ported)
        String pkg = traveler.getPackage_(); // Fixed getter
        if (pkg == null) pkg = "Standard Package";
        String pkgLower = pkg.toLowerCase();
        
        java.math.BigDecimal basePrice = new java.math.BigDecimal("149.00");
        
        if (pkgLower.contains("appointment only")) {
            basePrice = new java.math.BigDecimal("99.00");
        } else if (pkgLower.contains("full support") && !pkgLower.contains("fast track")) {
             basePrice = new java.math.BigDecimal("149.00");
        } else if (pkgLower.contains("fast track appointment")) {
             basePrice = new java.math.BigDecimal("199.00");
        } else if (pkgLower.contains("fast track full support")) {
             basePrice = new java.math.BigDecimal("349.00");
        }

        // 4. Line Items
        List<uk.co.visad.dto.InvoiceItemDto> items = new java.util.ArrayList<>();
        
        // Main Item
        String visaType = traveler.getVisaType();
        if (visaType == null) visaType = "Tourist Visa";
        String country = traveler.getTravelCountry();
        String desc = visaType + (hasText(country) ? " - " + country : "");
        
        items.add(uk.co.visad.dto.InvoiceItemDto.builder()
                .name(customerName + " - " + pkg)
                .description(desc)
                .price(basePrice)
                .quantity(1)
                .build());

        // Dependents Items
        List<uk.co.visad.entity.Dependent> dependents = dependentRepository.findByTraveler_Id(traveler.getId());
        for (uk.co.visad.entity.Dependent dep : dependents) {
            String depPkg = dep.getPackageType(); // Fixed getter
            if (depPkg == null || depPkg.isEmpty()) depPkg = pkg;
            String depPkgLower = depPkg.toLowerCase();
            
            java.math.BigDecimal depPrice = new java.math.BigDecimal("149.00");
             if (depPkgLower.contains("appointment only")) {
                depPrice = new java.math.BigDecimal("99.00");
            } else if (depPkgLower.contains("full support") && !depPkgLower.contains("fast track")) {
                 depPrice = new java.math.BigDecimal("149.00");
            } else if (depPkgLower.contains("fast track appointment")) {
                 depPrice = new java.math.BigDecimal("199.00");
            } else if (depPkgLower.contains("fast track full support")) {
                 depPrice = new java.math.BigDecimal("349.00");
            }

            String depName = getFullName(dep.getFirstName(), dep.getLastName());
            if (depName.isEmpty()) depName = "Co-Traveler";
            
            String depVisa = dep.getVisaType();   if(depVisa==null) depVisa = visaType;
            String depCtry = dep.getTravelCountry(); if(depCtry==null) depCtry = country;
            String depDesc = depVisa + (hasText(depCtry) ? " - " + depCtry : "") + " (Co-Traveler)";
            
             items.add(uk.co.visad.dto.InvoiceItemDto.builder()
                .name(depName + " - " + depPkg)
                .description(depDesc)
                .price(depPrice)
                .quantity(1)
                .build());
        }

        // 5. Validation & Totals
        java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
        for(uk.co.visad.dto.InvoiceItemDto item : items) {
            subtotal = subtotal.add(item.getPrice());
        }

        // Discount
        java.math.BigDecimal discountAmount = java.math.BigDecimal.ZERO;
        String discountLabel = "";
        
        String dType = traveler.getDiscountType(); 
        java.math.BigDecimal dVal = traveler.getDiscountValue(); // Fixed Type
        
        if ("percentage".equalsIgnoreCase(dType) && dVal != null && dVal.compareTo(java.math.BigDecimal.ZERO) > 0) {
            discountAmount = subtotal.multiply(dVal).divide(new java.math.BigDecimal("100"));
            discountLabel = "Discount (" + dVal.intValue() + "%)";
        } else if ("fixed".equalsIgnoreCase(dType) && dVal != null && dVal.compareTo(java.math.BigDecimal.ZERO) > 0) {
            discountAmount = dVal;
            discountLabel = "Discount";
        }
        
        if (discountAmount.compareTo(subtotal) > 0) discountAmount = subtotal;
        java.math.BigDecimal total = subtotal.subtract(discountAmount);

        // Check if invoice is generated/locked in DB, if so usage might differ, 
        // but for 'view' we usually calculate fresh OR use saved if we implemented that preference.
        // For now, let's return calculated.

        // History
        // Fetch last sent invoice dates manually or reuse getHistory logic
        List<InvoiceHistory> historyList = getHistory(id); // assuming traveler ID is record ID
        String lastInv = historyList.stream().filter(h -> "invoice".equals(h.getInvoiceType())).findFirst().map(h -> h.getSentAt().toString()).orElse(null);
        String lastTInv = historyList.stream().filter(h -> "t-invoice".equals(h.getInvoiceType())).findFirst().map(h -> h.getSentAt().toString()).orElse(null);

        return uk.co.visad.dto.InvoiceViewDto.builder()
                .invoiceNumber(invoiceNumber)
                .invoiceDate(now.format(dtf))
                .dueDate(now.plusDays(7).format(dtf))
                .customer(uk.co.visad.dto.InvoiceCustomerDto.builder()
                        .name(customerName)
                        .email(traveler.getEmail())
                        .addressLines(addressLines)
                        .build())
                .items(items)
                .subtotal(subtotal)
                .discount(discountAmount)
                .discountLabel(discountLabel)
                .total(total)
                .paymentStatus(traveler.getPaymentStatus())
                .status("Paid".equalsIgnoreCase(traveler.getPaymentStatus()) ? "Paid" : "Unpaid")
                .history(uk.co.visad.dto.InvoiceHistorySummaryDto.builder()
                        .lastSentInvoice(lastInv)
                        .lastSentTInvoice(lastTInv)
                        .build())
                .build();
    }
    
    @Transactional
    public void saveInvoice(Long travelerId, uk.co.visad.dto.InvoiceSaveRequestDto request) {
        uk.co.visad.entity.Traveler traveler = travelerRepository.findById(travelerId)
                .orElseThrow(() -> new RuntimeException("Traveler not found"));
        
        traveler.setInvoiceSubtotal(request.getSubtotal());
        traveler.setDiscountType(request.getDiscountType());
        traveler.setDiscountValue(request.getDiscountValue());
        
        // Also update invoice-specific fields
        traveler.setInvoiceDiscountType(request.getDiscountType());
        traveler.setInvoiceDiscountValue(request.getDiscountValue());
        traveler.setInvoiceDiscountAmount(request.getDiscountAmount());
        traveler.setInvoiceTotal(request.getTotal());
        traveler.setInvoiceItemsJson(request.getItemsJson());
        
        traveler.setInvoiceGenerated(true);
        traveler.setInvoiceGeneratedAt(java.time.LocalDateTime.now());
        
        travelerRepository.save(traveler);
    }
    private String getFullName(String first, String last) {
        if (first == null) first = "";
        if (last == null) last = "";
        return (first + " " + last).trim();
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String join(String delimiter, String s1, String s2) {
        List<String> list = new java.util.ArrayList<>();
        if (hasText(s1)) list.add(s1);
        if (hasText(s2)) list.add(s2);
        return String.join(delimiter, list);
    }
}

