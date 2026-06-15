/**
 * Modern Admin Dashboard JavaScript
 * Enhanced functionality for Customer Service Dashboard
 */

class AdminDashboard {
  constructor() {
    this.init();
  }

  init() {
    this.setupEventListeners();
    this.initializeAnimations();
    this.setupTooltips();
    this.loadDashboardData();
  }

  /**
   * Setup all event listeners
   */
  setupEventListeners() {
    // Refresh dashboard button
    const refreshBtn = document.querySelector('[onclick="refreshDashboard()"]');
    if (refreshBtn) {
      refreshBtn.addEventListener("click", this.refreshDashboard.bind(this));
    }

    // Help button
    const helpBtn = document.querySelector('[onclick="showDashboardHelp()"]');
    if (helpBtn) {
      helpBtn.addEventListener("click", this.showDashboardHelp.bind(this));
    }

    // View details buttons
    const viewDetailsBtns = document.querySelectorAll('[onclick*="show"]');
    viewDetailsBtns.forEach((btn) => {
      const onclick = btn.getAttribute("onclick");
      if (onclick.includes("showRecentClosures")) {
        btn.addEventListener("click", this.showRecentClosures.bind(this));
      } else if (onclick.includes("showPendingNotifications")) {
        btn.addEventListener("click", this.showPendingNotifications.bind(this));
      } else if (onclick.includes("showWinnersNotified")) {
        btn.addEventListener("click", this.showWinnersNotified.bind(this));
      } else if (onclick.includes("showOpenQueries")) {
        btn.addEventListener("click", this.showOpenQueries.bind(this));
      }
    });

    // Auto-refresh every 5 minutes
    setInterval(() => {
      this.refreshDashboard();
    }, 300000);

    // Setup smooth scrolling for anchor links
    this.setupSmoothScrolling();
  }

  /**
   * Initialize animations and transitions
   */
  initializeAnimations() {
    // Add fade-in animation to cards
    const cards = document.querySelectorAll(".stat-card, .content-card");
    cards.forEach((card, index) => {
      card.style.opacity = "0";
      card.style.transform = "translateY(30px)";

      setTimeout(() => {
        card.style.transition = "all 0.6s ease-out";
        card.style.opacity = "1";
        card.style.transform = "translateY(0)";
      }, index * 100);
    });

    // Add hover effects to interactive elements
    this.addHoverEffects();
  }

  /**
   * Add hover effects to interactive elements
   */
  addHoverEffects() {
    const interactiveElements = document.querySelectorAll(
      ".btn-modern, .stat-card, .content-card"
    );

    interactiveElements.forEach((element) => {
      element.addEventListener("mouseenter", () => {
        element.style.transform = "translateY(-5px)";
      });

      element.addEventListener("mouseleave", () => {
        element.style.transform = "translateY(0)";
      });
    });
  }

  /**
   * Setup tooltips
   */
  setupTooltips() {
    // Initialize Bootstrap tooltips
    const tooltipTriggerList = [].slice.call(
      document.querySelectorAll('[data-bs-toggle="tooltip"]')
    );
    tooltipTriggerList.map(function (tooltipTriggerEl) {
      return new bootstrap.Tooltip(tooltipTriggerEl);
    });
  }

  /**
   * Load dashboard data
   */
  async loadDashboardData() {
    try {
      // Show loading state
      this.showLoadingState();

      // Simulate API call
      await new Promise((resolve) => setTimeout(resolve, 1000));

      // Hide loading state
      this.hideLoadingState();

      // Update dashboard with real data
      this.updateDashboardStats();
    } catch (error) {
      console.error("Error loading dashboard data:", error);
      this.showNotification(
        "error",
        "Loading Error",
        "Failed to load dashboard data"
      );
    }
  }

  /**
   * Show loading state
   */
  showLoadingState() {
    const cards = document.querySelectorAll(".stat-card");
    cards.forEach((card) => {
      const numberElement = card.querySelector(".stat-number");
      if (numberElement) {
        numberElement.classList.add("loading-shimmer");
        numberElement.textContent = "";
      }
    });
  }

  /**
   * Hide loading state
   */
  hideLoadingState() {
    const cards = document.querySelectorAll(".stat-card");
    cards.forEach((card) => {
      const numberElement = card.querySelector(".stat-number");
      if (numberElement) {
        numberElement.classList.remove("loading-shimmer");
      }
    });
  }

  /**
   * Update dashboard statistics
   */
  updateDashboardStats() {
    // This would typically fetch real data from the server
    // For now, we'll just ensure the Thymeleaf data is displayed properly
    console.log("Dashboard stats updated");
  }

  /**
   * Refresh dashboard
   */
  async refreshDashboard() {
    const refreshBtn = document.querySelector('[onclick="refreshDashboard()"]');
    if (refreshBtn) {
      this.setButtonLoading(refreshBtn, true);
    }

    try {
      // Simulate refresh
      await new Promise((resolve) => setTimeout(resolve, 1500));

      // Reload the page to get fresh data
      window.location.reload();
    } catch (error) {
      console.error("Error refreshing dashboard:", error);
      this.showNotification(
        "error",
        "Refresh Error",
        "Failed to refresh dashboard"
      );
    } finally {
      if (refreshBtn) {
        this.setButtonLoading(refreshBtn, false);
      }
    }
  }

  /**
   * Show dashboard help
   */
  showDashboardHelp() {
    const modal = document.createElement("div");
    modal.className = "modal fade modal-modern";
    modal.innerHTML = `
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">
              <i class="fas fa-question-circle me-2"></i>Dashboard Help
            </h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <div class="row">
              <div class="col-md-6">
                <h6><i class="fas fa-chart-bar me-2"></i>Statistics Overview</h6>
                <ul class="list-unstyled">
                  <li><i class="fas fa-gavel me-2 text-primary"></i>Recent Closures: Auctions closed in the last 7 days</li>
                  <li><i class="fas fa-bell me-2 text-warning"></i>Pending Notifications: Notifications requiring action</li>
                  <li><i class="fas fa-trophy me-2 text-success"></i>Winners Notified: Successful winner notifications</li>
                  <li><i class="fas fa-question-circle me-2 text-info"></i>Open Queries: Customer service queries pending</li>
                </ul>
              </div>
              <div class="col-md-6">
                <h6><i class="fas fa-cogs me-2"></i>Quick Actions</h6>
                <ul class="list-unstyled">
                  <li><i class="fas fa-sync-alt me-2 text-primary"></i>Refresh Dashboard: Update all data</li>
                  <li><i class="fas fa-eye me-2 text-success"></i>View Details: See detailed information</li>
                  <li><i class="fas fa-download me-2 text-info"></i>Export Data: Download reports</li>
                  <li><i class="fas fa-settings me-2 text-warning"></i>Settings: Configure dashboard</li>
                </ul>
              </div>
            </div>
            <div class="alert alert-info-modern mt-3">
              <i class="fas fa-info-circle me-2"></i>
              <strong>Tip:</strong> Click on any statistic card to view detailed information and take actions.
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-primary-modern btn-modern" data-bs-dismiss="modal">
              <i class="fas fa-check me-2"></i>Got it!
            </button>
          </div>
        </div>
      </div>
    `;

    document.body.appendChild(modal);
    new bootstrap.Modal(modal).show();

    // Clean up modal after it's hidden
    modal.addEventListener("hidden.bs.modal", () => {
      document.body.removeChild(modal);
    });
  }

  /**
   * Show recent closures
   */
  showRecentClosures() {
    this.showNotification(
      "info",
      "Recent Closures",
      "Loading recent auction closures..."
    );
    // Implementation would fetch and display recent closures
  }

  /**
   * Show pending notifications
   */
  showPendingNotifications() {
    this.showNotification(
      "warning",
      "Pending Notifications",
      "Loading pending notifications..."
    );
    // Implementation would fetch and display pending notifications
  }

  /**
   * Show winners notified
   */
  showWinnersNotified() {
    this.showNotification(
      "success",
      "Winners Notified",
      "Loading winner notifications..."
    );
    // Implementation would fetch and display winner notifications
  }

  /**
   * Show open queries
   */
  showOpenQueries() {
    this.showNotification(
      "info",
      "Open Queries",
      "Loading open customer queries..."
    );
    // Implementation would fetch and display open queries
  }

  /**
   * Set button loading state
   */
  setButtonLoading(button, isLoading) {
    if (!button) return;

    if (isLoading) {
      button.disabled = true;
      button.innerHTML =
        '<span class="spinner-border spinner-border-sm me-2"></span>Loading...';
    } else {
      button.disabled = false;
      button.innerHTML =
        '<i class="fas fa-sync-alt me-2"></i>Refresh Dashboard';
    }
  }

  /**
   * Show notification
   */
  showNotification(type, title, message) {
    const existingAlerts = document.querySelectorAll(".alert-dismissible");
    existingAlerts.forEach((alert) => alert.remove());

    const alertDiv = document.createElement("div");
    alertDiv.className = `alert alert-${type}-modern alert-dismissible fade show position-fixed`;
    alertDiv.style.cssText =
      "top: 100px; right: 20px; z-index: 9999; min-width: 300px;";
    alertDiv.innerHTML = `
      <div class="d-flex align-items-center">
        <i class="fas fa-${this.getIconForType(type)} me-2"></i>
        <div>
          <strong>${title}</strong><br>
          ${message}
        </div>
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
      </div>
    `;

    document.body.appendChild(alertDiv);

    setTimeout(() => {
      if (alertDiv.parentNode) {
        alertDiv.remove();
      }
    }, 5000);
  }

  /**
   * Get icon for notification type
   */
  getIconForType(type) {
    const icons = {
      success: "check-circle",
      error: "exclamation-circle",
      warning: "exclamation-triangle",
      info: "info-circle",
    };
    return icons[type] || "info-circle";
  }

  /**
   * Setup smooth scrolling
   */
  setupSmoothScrolling() {
    document.querySelectorAll('a[href^="#"]').forEach((anchor) => {
      anchor.addEventListener("click", function (e) {
        e.preventDefault();
        const targetId = this.getAttribute("href").substring(1);
        const targetElement = document.getElementById(targetId);
        if (targetElement) {
          targetElement.scrollIntoView({
            behavior: "smooth",
            block: "start",
          });
        }
      });
    });
  }

  /**
   * Export dashboard data
   */
  exportDashboardData() {
    this.showNotification(
      "info",
      "Export Started",
      "Preparing dashboard data for export..."
    );
    // Implementation would export dashboard data
  }

  /**
   * Print dashboard
   */
  printDashboard() {
    window.print();
  }
}

// Initialize when DOM is loaded
document.addEventListener("DOMContentLoaded", () => {
  new AdminDashboard();
});

// Export for global access
window.AdminDashboard = AdminDashboard;
