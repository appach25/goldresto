package com.goldresto.service;

import com.goldresto.entity.*;
import com.goldresto.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;

@Service
public class ReportService {

    @Autowired
    private PanierRepository panierRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private LignedeProduitRepository lignedeProduitRepository;

    @Autowired
    private UserRepository userRepository;

    public Map<String, Object> getDailySalesReport(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Panier> paniers = panierRepository.findByDateBetween(start, end);
        if (paniers == null) {
            paniers = new ArrayList<>();
        }

        // Filter for only paid orders
        paniers = paniers.stream()
            .filter(p -> p != null && p.getState() == PanierState.PAYER)
            .collect(Collectors.toList());
        
        Map<String, Object> report = new HashMap<>();
        report.put("date", date);
        report.put("totalSales", calculateTotalSales(paniers));
        report.put("orderCount", paniers.size());
        report.put("revenue", calculateRevenue(paniers));
        report.put("hourlyBreakdown", getHourlyBreakdown(paniers));

        // Add sales list for detailed view
        List<Map<String, Object>> sales = paniers.stream()
            .map(p -> {
                Map<String, Object> sale = new HashMap<>();
                sale.put("time", p.getDate());
                sale.put("table", p.getNumeroTable());
                sale.put("amount", p.getTotal());
                return sale;
            })
            .sorted(Comparator.comparing(m -> ((LocalDateTime)m.get("time"))))
            .collect(Collectors.toList());
        report.put("sales", sales);
        
        return report;
    }

    public Map<String, Object> getPeriodSalesReport(LocalDate startDate, LocalDate endDate, String periodType) {
        // Initialize with default values
        Map<String, Object> report = new HashMap<>();
        report.put("revenue", BigDecimal.ZERO);
        report.put("orderCount", 0L);
        report.put("trends", new HashMap<String, BigDecimal>());
        report.put("previousPeriod", null);

        // Ensure we have valid dates
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        // Add dates to report
        report.put("startDate", startDate);
        report.put("endDate", endDate);

        List<Panier> paniers = panierRepository.findByDateBetween(
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        );

        if (paniers == null || paniers.isEmpty()) {
            return report;
        }
        
        if (paniers == null) {
            paniers = new ArrayList<>();
        }
        
        // Filter for only paid orders and valid dates
        final LocalDate finalStartDate = startDate;
        final LocalDate finalEndDate = endDate;
        paniers = paniers.stream()
            .filter(p -> p != null && p.getState() == PanierState.PAYER && p.getDate() != null)
            .filter(p -> {
                LocalDateTime orderDate = p.getDate();
                return !orderDate.toLocalDate().isBefore(finalStartDate) && !orderDate.toLocalDate().isAfter(finalEndDate);
            })
            .collect(Collectors.toList());

        report.put("totalSales", calculateTotalSales(paniers));
        report.put("orderCount", paniers.size());
        report.put("revenue", calculateRevenue(paniers));
        
        // Add period comparison with previous period
        LocalDate previousStartDate = getPreviousPeriodStart(startDate, periodType);
        LocalDate previousEndDate = getPreviousPeriodEnd(endDate, periodType);
        List<Panier> previousPaniers = panierRepository.findByDateBetween(
            previousStartDate.atStartOfDay(),
            previousEndDate.plusDays(1).atStartOfDay()
        );
        
        // Handle previous period data
        if (previousPaniers != null && !previousPaniers.isEmpty()) {
            Map<String, Object> previousPeriodMap = new HashMap<>();
            previousPeriodMap.put("startDate", previousStartDate);
            previousPeriodMap.put("endDate", previousEndDate);
            previousPeriodMap.put("totalSales", calculateTotalSales(previousPaniers));
            previousPeriodMap.put("orderCount", previousPaniers.size());
            previousPeriodMap.put("revenue", calculateRevenue(previousPaniers));
            report.put("previousPeriod", previousPeriodMap);
        } else {
            report.put("previousPeriod", null);
        }
        
        // Add trend data
        report.put("trends", calculateTrends(paniers, periodType));
        
        return report;
    }

    public Map<String, Object> getProductSalesReport(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();

        // Ensure we have valid dates
        if (startDate == null || endDate == null) {
            startDate = LocalDate.now().minusMonths(1);
            endDate = LocalDate.now();
        }

        // Add dates to report
        report.put("startDate", startDate);
        report.put("endDate", endDate);

        // Get all product lines between dates
        List<LignedeProduit> lignes = lignedeProduitRepository.findByPanierDateBetween(
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        );

        if (lignes == null) {
            lignes = new ArrayList<>();
        }

        // Filter for only paid orders and valid dates
        final LocalDate finalStartDate = startDate;
        final LocalDate finalEndDate = endDate;
        lignes = lignes.stream()
            .filter(l -> l != null && l.getPanier() != null && 
                    l.getPanier().getState() == PanierState.PAYER && 
                    l.getPanier().getDate() != null)
            .filter(l -> {
                LocalDateTime orderDate = l.getPanier().getDate();
                return !orderDate.toLocalDate().isBefore(finalStartDate) && 
                       !orderDate.toLocalDate().isAfter(finalEndDate);
            })
            .collect(Collectors.toList());

        // Group by product and calculate metrics
        Map<Produit, ProductMetrics> productMetrics = new HashMap<>();
        
        for (LignedeProduit ligne : lignes) {
            productMetrics.computeIfAbsent(ligne.getProduit(), k -> new ProductMetrics())
                .addSale(ligne.getQuantite(), ligne.getSousTotal());
        }

        // Calculate total revenue and add trend data
        BigDecimal totalRevenue = productMetrics.values().stream()
            .map(ProductMetrics::getRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Add total revenue to report
        report.put("totalRevenue", totalRevenue);

        // Convert to list and sort by revenue
        List<Map.Entry<Produit, ProductMetrics>> sortedProducts = productMetrics.entrySet().stream()
            .sorted((a, b) -> b.getValue().getRevenue().compareTo(a.getValue().getRevenue()))
            .collect(Collectors.toList());

        // Calculate percentage of total for each product
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            sortedProducts.forEach(entry -> {
                ProductMetrics metrics = entry.getValue();
                metrics.setPercentageOfTotal(
                    metrics.getRevenue()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalRevenue, 2, BigDecimal.ROUND_HALF_UP)
                );
            });
        }

        // Function to convert entry to product map
        Function<Map.Entry<Produit, ProductMetrics>, Map<String, Object>> toProductMap = entry -> {
            Map<String, Object> product = new HashMap<>();
            product.put("product", entry.getKey());
            product.put("quantitySold", entry.getValue().getQuantitySold());
            product.put("revenue", entry.getValue().getRevenue());
            product.put("percentageOfTotal", entry.getValue().getPercentageOfTotal());
            return product;
        };

        // Get top 10 products (or empty list if no products)
        List<Map<String, Object>> topProducts = sortedProducts.isEmpty() ? new ArrayList<>() : 
            sortedProducts.stream()
                .limit(10)
                .map(toProductMap)
                .collect(Collectors.toList());

        // Get bottom 5 products (or empty list if no products)
        List<Map<String, Object>> lowPerformers = sortedProducts.isEmpty() ? new ArrayList<>() :
            sortedProducts.stream()
                .skip(Math.max(0, sortedProducts.size() - 5))
                .map(toProductMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("topProducts", topProducts);
        result.put("lowPerformers", lowPerformers);
        return result;
    }

    public Map<String, Object> getCategorySalesReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            startDate = LocalDate.now().minusMonths(1);
            endDate = LocalDate.now();
        }

        List<LignedeProduit> lignes = lignedeProduitRepository.findByPanierDateBetween(
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        );

        if (lignes == null) {
            lignes = new ArrayList<>();
        }

        // Filter for only paid orders
        lignes = lignes.stream()
            .filter(l -> l != null && l.getPanier() != null && 
                    l.getPanier().getState() == PanierState.PAYER)
            .collect(Collectors.toList());

        // Group by category
        Map<String, CategoryMetrics> categoryMetrics = new HashMap<>();
        
        for (LignedeProduit ligne : lignes) {
            Produit produit = ligne.getProduit();
            String category = produit != null ? produit.getCategorie() : "Non catégorisé";
            categoryMetrics.computeIfAbsent(category, k -> new CategoryMetrics())
                .addSale(ligne.getQuantite(), ligne.getSousTotal());
        }

        // Sort categories by revenue
        final List<Map.Entry<String, CategoryMetrics>> sortedCategories = new ArrayList<>(categoryMetrics.entrySet());
        sortedCategories.sort((a, b) -> b.getValue().getRevenue().compareTo(a.getValue().getRevenue()));

        // Calculate total revenue once
        final BigDecimal categoryTotalRevenue = calculateTotalRevenue(lignes);

        // Function to convert entry to category map
        Function<Map.Entry<String, CategoryMetrics>, Map<String, Object>> toCategoryMap = entry -> {
            Map<String, Object> category = new HashMap<>();
            category.put("category", entry.getKey());
            category.put("quantity", entry.getValue().getItemsSold());
            category.put("revenue", entry.getValue().getRevenue());
            category.put("percentageOfTotal", categoryTotalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                entry.getValue().getRevenue().multiply(BigDecimal.valueOf(100)).divide(categoryTotalRevenue, 2, BigDecimal.ROUND_HALF_UP) :
                BigDecimal.ZERO);
            return category;
        };

        // Convert to list of maps for the view
        List<Map<String, Object>> categoryPerformance = sortedCategories.stream()
            .map(toCategoryMap)
            .sorted((m1, m2) -> ((BigDecimal)m2.get("revenue")).compareTo((BigDecimal)m1.get("revenue")))
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("categoryPerformance", categoryPerformance);
        return result;
    }

    public Map<String, Object> getEmployeePerformanceReport(LocalDate startDate, LocalDate endDate) {
        // Initialize with default dates if not provided
        if (startDate == null || endDate == null) {
            startDate = LocalDate.now().minusMonths(1);
            endDate = LocalDate.now();
        }

        // Get orders for the period
        List<Panier> paniers = panierRepository.findByDateBetween(
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        );

        // Initialize empty report if no orders
        if (paniers == null || paniers.isEmpty()) {
            Map<String, Object> report = new HashMap<>();
            report.put("startDate", startDate);
            report.put("endDate", endDate);
            report.put("employeePerformance", new ArrayList<>());
            return report;
        }

        // Filter for only paid orders
        paniers = paniers.stream()
            .filter(p -> p != null && p.getState() == PanierState.PAYER)
            .collect(Collectors.toList());

        // Group by employee
        Map<User, EmployeeMetrics> employeeMetrics = new HashMap<>();
        
        for (Panier panier : paniers) {
            User employee = panier.getUser();
            if (employee == null) {
                employee = new User();
                employee.setId(-1L);
                employee.setUsername("unknown");
                employee.setFullName("Unknown Employee");
                employee.setPassword("");
            }
            
            employeeMetrics.computeIfAbsent(employee, k -> new EmployeeMetrics())
                .addOrder(panier.getTotal());
        }

        // Convert to list and sort by revenue
        List<Map<String, Object>> employeePerformance = employeeMetrics.entrySet().stream()
            .map(entry -> {
                Map<String, Object> employeeMap = new HashMap<>();
                employeeMap.put("employee", entry.getKey());
                employeeMap.put("ordersProcessed", entry.getValue().getOrdersProcessed());
                employeeMap.put("totalRevenue", entry.getValue().getTotalRevenue());
                return employeeMap;
            })
            .sorted((m1, m2) -> ((BigDecimal)m2.get("totalRevenue")).compareTo((BigDecimal)m1.get("totalRevenue")))
            .collect(Collectors.toList());

        // Build final report
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("employeePerformance", employeePerformance);
        return result;
    }

    // Helper classes for metrics
    private static class ProductMetrics {
        private int quantitySold = 0;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal percentageOfTotal = BigDecimal.ZERO;

        void addSale(int quantity, BigDecimal amount) {
            quantitySold += quantity;
            revenue = revenue.add(amount);
        }

        int getQuantitySold() { return quantitySold; }
        BigDecimal getRevenue() { return revenue; }
        BigDecimal getPercentageOfTotal() { return percentageOfTotal; }
        void setPercentageOfTotal(BigDecimal percentageOfTotal) { this.percentageOfTotal = percentageOfTotal; }
    }

    private static class CategoryMetrics {
        private int itemsSold = 0;
        private BigDecimal revenue = BigDecimal.ZERO;

        void addSale(int quantity, BigDecimal amount) {
            itemsSold += quantity;
            revenue = revenue.add(amount);
        }

        int getItemsSold() { return itemsSold; }
        BigDecimal getRevenue() { return revenue; }
    }

    private static class EmployeeMetrics {
        private int ordersProcessed = 0;
        private BigDecimal totalRevenue = BigDecimal.ZERO;

        void addOrder(BigDecimal amount) {
            ordersProcessed++;
            if (amount != null) {
                totalRevenue = totalRevenue.add(amount);
            }
        }

        int getOrdersProcessed() { return ordersProcessed; }
        BigDecimal getTotalRevenue() { return totalRevenue; }
    }

    // Helper methods
    private BigDecimal calculateTotalSales(List<Panier> paniers) {
        if (paniers == null || paniers.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return paniers.stream()
            .filter(p -> p != null && p.getState() == PanierState.PAYER)
            .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateRevenue(List<Panier> paniers) {
        return calculateTotalSales(paniers);
    }

    private BigDecimal calculateTotalRevenue(List<LignedeProduit> lignes) {
        if (lignes == null) {
            return BigDecimal.ZERO;
        }
        return lignes.stream()
            .filter(l -> l != null && l.getSousTotal() != null)
            .map(LignedeProduit::getSousTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Integer, BigDecimal> getHourlyBreakdown(List<Panier> paniers) {
        Map<Integer, BigDecimal> hourlyTotals = new HashMap<>();
        if (paniers == null) {
            return hourlyTotals;
        }
        for (Panier panier : paniers) {
            if (panier.getDate() != null && panier.getTotal() != null) {
                int hour = panier.getDate().getHour();
                hourlyTotals.merge(hour, panier.getTotal(), BigDecimal::add);
            }
        }
        return hourlyTotals;
    }

    private Map<String, BigDecimal> calculateTrends(List<Panier> paniers, String periodType) {
        Map<String, BigDecimal> trends = new TreeMap<>();
        
        try {
            if (paniers == null || periodType == null) {
                return trends;
            }
            
            // Group orders by the appropriate period, with additional null checks
            Map<LocalDate, BigDecimal> dailyTotals = paniers.stream()
                .filter(p -> p != null && p.getDate() != null && p.getTotal() != null)
                .collect(Collectors.groupingBy(
                    p -> p.getDate().toLocalDate(),
                    Collectors.mapping(
                        p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO,
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                    )
                ));
            
            if (dailyTotals.isEmpty()) {
                return trends;
            }
        
            // Get the date range
            LocalDate minDate = Collections.min(dailyTotals.keySet());
            LocalDate maxDate = Collections.max(dailyTotals.keySet());
            
            // Generate periods based on periodType
            switch (periodType.toLowerCase()) {
                case "day" -> {
                    for (LocalDate date = minDate; !date.isAfter(maxDate); date = date.plusDays(1)) {
                        String key = date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                        trends.put(key, dailyTotals.getOrDefault(date, BigDecimal.ZERO));
                    }
                }
                case "week" -> {
                    try {
                        Map<Integer, BigDecimal> weeklyTotals = new TreeMap<>();
                        dailyTotals.forEach((date, total) -> {
                            if (date != null && total != null) {
                                int weekNumber = date.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
                                weeklyTotals.merge(weekNumber, total, (a, b) -> a.add(b != null ? b : BigDecimal.ZERO));
                            }
                        });
                        weeklyTotals.forEach((week, total) -> 
                            trends.put("Semaine " + week, total != null ? total : BigDecimal.ZERO));
                    } catch (Exception e) {
                        trends.put("Erreur", BigDecimal.ZERO);
                    }
                }
                case "month" -> {
                    for (LocalDate date = minDate.withDayOfMonth(1); 
                         !date.isAfter(maxDate); 
                         date = date.plusMonths(1)) {
                        final LocalDate periodDate = date;
                        BigDecimal monthTotal = dailyTotals.entrySet().stream()
                            .filter(e -> e.getKey().getYear() == periodDate.getYear() 
                                     && e.getKey().getMonth() == periodDate.getMonth())
                            .map(Map.Entry::getValue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        String key = date.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));
                        trends.put(key, monthTotal);
                    }
                }
                default -> {
                    // Default to daily view
                    for (LocalDate date = minDate; !date.isAfter(maxDate); date = date.plusDays(1)) {
                        String key = date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                        trends.put(key, dailyTotals.getOrDefault(date, BigDecimal.ZERO));
                    }
                }
            }
        } catch (Exception e) {
            // Log or handle error if needed
        }
        
        return trends;
    }

    private LocalDate getPreviousPeriodStart(LocalDate date, String periodType) {
        return switch (periodType.toLowerCase()) {
            case "day" -> date.minusDays(1);
            case "week" -> date.minusWeeks(1);
            case "month" -> date.minusMonths(1);
            case "year" -> date.minusYears(1);
            default -> date.minusDays(1);
        };
    }

    private LocalDate getPreviousPeriodEnd(LocalDate date, String periodType) {
        return switch (periodType.toLowerCase()) {
            case "day" -> date.minusDays(1);
            case "week" -> date.minusWeeks(1);
            case "month" -> date.minusMonths(1);
            case "year" -> date.minusYears(1);
            default -> date.minusDays(1);
        };
    }
}
