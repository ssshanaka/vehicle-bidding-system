/* Footer Fragment JavaScript - Lanka Auto Traders */

// Footer-specific initialization
document.addEventListener("DOMContentLoaded", function () {
  initializeFooter();
});

// Initialize footer functionality
function initializeFooter() {
  // Initialize social links
  initializeSocialLinks();

  // Initialize footer links
  initializeFooterLinks();

  // Initialize contact information
  initializeContactInfo();

  // Update current date
  updateCurrentDate();

  // Initialize animations
  initializeFooterAnimations();
}

// Initialize social links with hover effects
function initializeSocialLinks() {
  const socialLinks = document.querySelectorAll(".social-link");

  socialLinks.forEach(function (link) {
    link.addEventListener("mouseenter", function () {
      this.style.transform = "translateY(-3px)";
    });

    link.addEventListener("mouseleave", function () {
      this.style.transform = "translateY(0)";
    });

    // Add click tracking
    link.addEventListener("click", function (e) {
      e.preventDefault();
      const platform = this.getAttribute("title") || "Social Media";
      console.log(`Social link clicked: ${platform}`);

      // You can add analytics tracking here
      // trackSocialClick(platform);
    });
  });
}

// Initialize footer links with smooth scrolling
function initializeFooterLinks() {
  const footerLinks = document.querySelectorAll(".footer-link");

  footerLinks.forEach(function (link) {
    link.addEventListener("click", function (e) {
      const href = this.getAttribute("href");

      // Handle internal links
      if (href && href.startsWith("#")) {
        e.preventDefault();
        const targetElement = document.querySelector(href);
        if (targetElement) {
          targetElement.scrollIntoView({
            behavior: "smooth",
            block: "start",
          });
        }
      }

      // Handle modal triggers
      if (href === "#" && this.getAttribute("data-bs-toggle") === "modal") {
        // Let Bootstrap handle the modal
        return;
      }
    });
  });
}

// Initialize contact information interactions
function initializeContactInfo() {
  const contactItems = document.querySelectorAll(".contact-item");

  contactItems.forEach(function (item) {
    item.addEventListener("mouseenter", function () {
      this.style.transform = "translateX(5px)";
    });

    item.addEventListener("mouseleave", function () {
      this.style.transform = "translateX(0)";
    });

    // Add click handlers for contact actions
    const contactIcon = item.querySelector(".contact-icon");
    if (contactIcon) {
      contactIcon.addEventListener("click", function () {
        const contactType = this.classList.contains("bi-geo-alt-fill")
          ? "address"
          : this.classList.contains("bi-telephone-fill")
          ? "phone"
          : this.classList.contains("bi-envelope-fill")
          ? "email"
          : "unknown";

        handleContactClick(contactType, item);
      });
    }
  });
}

// Handle contact information clicks
function handleContactClick(type, element) {
  switch (type) {
    case "phone":
      const phoneNumber = element.querySelector(".contact-value").textContent;
      if (phoneNumber) {
        // Copy phone number to clipboard
        navigator.clipboard
          .writeText(phoneNumber)
          .then(function () {
            showFooterNotification(
              "Phone number copied to clipboard!",
              "success"
            );
          })
          .catch(function () {
            showFooterNotification("Unable to copy phone number", "warning");
          });
      }
      break;

    case "email":
      const email = element.querySelector(".contact-value").textContent;
      if (email) {
        // Copy email to clipboard
        navigator.clipboard
          .writeText(email)
          .then(function () {
            showFooterNotification("Email copied to clipboard!", "success");
          })
          .catch(function () {
            showFooterNotification("Unable to copy email", "warning");
          });
      }
      break;

    case "address":
      const address = element.querySelector(".contact-value").textContent;
      if (address) {
        // Open maps with address
        const mapsUrl = `https://maps.google.com/?q=${encodeURIComponent(
          address
        )}`;
        window.open(mapsUrl, "_blank");
      }
      break;
  }
}

