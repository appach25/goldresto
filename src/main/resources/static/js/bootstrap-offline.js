/**
 * Bootstrap JavaScript simplifié pour travail hors ligne
 * Fonctionnalités de base nécessaires
 */

// Bootstrap Collapse
(function() {
    'use strict';
    
    // Gestion des dropdowns/toggles
    document.addEventListener('click', function(event) {
        const toggle = event.target.closest('[data-bs-toggle="collapse"], [data-bs-toggle="dropdown"]');
        if (!toggle) return;
        
        event.preventDefault();
        
        const target = toggle.getAttribute('data-bs-target') || toggle.getAttribute('href');
        if (!target) return;
        
        const targetElement = document.querySelector(target);
        if (!targetElement) return;
        
        // Toggle collapse
        if (targetElement.classList.contains('show')) {
            targetElement.classList.remove('show');
            targetElement.style.height = '0px';
            setTimeout(() => {
                targetElement.style.display = 'none';
            }, 350);
        } else {
            targetElement.style.display = 'block';
            targetElement.style.height = 'auto';
            const height = targetElement.scrollHeight + 'px';
            targetElement.style.height = '0px';
            setTimeout(() => {
                targetElement.style.height = height;
                targetElement.classList.add('show');
            }, 10);
            setTimeout(() => {
                targetElement.style.height = '';
            }, 360);
        }
    });
    
    // Gestion des tabs
    document.addEventListener('click', function(event) {
        if (!event.target.matches('[data-bs-toggle="tab"]')) return;
        
        event.preventDefault();
        const tab = event.target;
        
        // Désactiver tous les tabs du même conteneur
        const tabContainer = tab.closest('.nav-tabs');
        if (tabContainer) {
            tabContainer.querySelectorAll('.nav-link').forEach(t => {
                t.classList.remove('active');
            });
        }
        
        // Activer le tab cliqué
        tab.classList.add('active');
        
        // Masquer tous les contenus
        const targetId = tab.getAttribute('href') || tab.getAttribute('data-bs-target');
        document.querySelectorAll('.tab-pane').forEach(pane => {
            pane.classList.remove('show', 'active');
        });
        
        // Afficher le contenu cible
        const targetPane = document.querySelector(targetId);
        if (targetPane) {
            targetPane.classList.add('show', 'active');
        }
    });
    
    // Gestion des modals (simplifié)
    window.BootstrapModal = {
        show: function(modalId) {
            const modal = document.getElementById(modalId);
            if (modal) {
                modal.style.display = 'block';
                modal.classList.add('show');
                document.body.classList.add('modal-open');
            }
        },
        
        hide: function(modalId) {
            const modal = document.getElementById(modalId);
            if (modal) {
                modal.style.display = 'none';
                modal.classList.remove('show');
                document.body.classList.remove('modal-open');
            }
        }
    };
    
    // Auto-hide pour les alerts
    setTimeout(() => {
        document.querySelectorAll('.alert-dismissible').forEach(alert => {
            const closeBtn = alert.querySelector('.btn-close');
            if (closeBtn) {
                closeBtn.addEventListener('click', () => {
                    alert.style.display = 'none';
                });
            }
        });
    }, 100);
})();

// Fonctions utilitaires
window.BootstrapUtils = {
    // Afficher un toast notification
    showToast: function(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
        toast.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
        toast.innerHTML = `
            ${message}
            <button type="button" class="btn-close" onclick="this.parentElement.style.display='none'"></button>
        `;
        document.body.appendChild(toast);
        
        setTimeout(() => {
            toast.style.display = 'none';
            setTimeout(() => {
                document.body.removeChild(toast);
            }, 300);
        }, 5000);
    },
    
    // Valider un formulaire
    validateForm: function(formId) {
        const form = document.getElementById(formId);
        if (!form) return false;
        
        const inputs = form.querySelectorAll('input[required], select[required], textarea[required]');
        let isValid = true;
        
        inputs.forEach(input => {
            if (!input.value.trim()) {
                input.classList.add('is-invalid');
                isValid = false;
            } else {
                input.classList.remove('is-invalid');
            }
        });
        
        return isValid;
    }
};

// Initialisation automatique
document.addEventListener('DOMContentLoaded', function() {
    // Ajouter les styles pour les transitions
    const style = document.createElement('style');
    style.textContent = `
        .collapse {
            transition: height 0.35s ease;
            overflow: hidden;
        }
        .collapse.show {
            height: auto !important;
        }
        .tab-pane {
            display: none;
        }
        .tab-pane.show.active {
            display: block;
        }
        .is-invalid {
            border-color: #dc3545 !important;
        }
        .modal {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0,0,0,0.5);
            z-index: 1050;
        }
        .modal.show {
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .modal-content {
            background: white;
            padding: 20px;
            border-radius: 8px;
            max-width: 500px;
            width: 90%;
        }
        .btn-close {
            background: none;
            border: none;
            font-size: 1.5rem;
            cursor: pointer;
            opacity: 0.5;
        }
        .btn-close:hover {
            opacity: 1;
        }
    `;
    document.head.appendChild(style);
});

console.log('Bootstrap Offline chargé');
