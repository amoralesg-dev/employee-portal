document.addEventListener('DOMContentLoaded', () => {
    const loginScreen = document.getElementById('login-screen');
    const dashboardScreen = document.getElementById('dashboard-screen');
    const loginForm = document.getElementById('login-form');
    const errorMsg = document.getElementById('login-error');
    const dynamicMenu = document.getElementById('dynamic-menu');
    const pageTitle = document.getElementById('page-title');
    const mainContentArea = document.getElementById('main-content-area');

    // Comprobar si hay token activo
    const token = localStorage.getItem('accessToken');
    if (token) {
        fetchUserData(token);
    }

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        const btn = document.getElementById('login-btn');
        
        errorMsg.textContent = '';
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Cargando...';
        btn.disabled = true;

        try {
            const res = await fetch('/api/v1/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            if (!res.ok) {
                throw new Error('Credenciales inválidas o cuenta inactiva');
            }

            const data = await res.json();
            localStorage.setItem('accessToken', data.accessToken);
            if (data.refreshToken) {
                localStorage.setItem('refreshToken', data.refreshToken);
            }
            
            // Ya tenemos el payload, podemos renderizar directo, pero para cumplir con 
            // la instrucción "construirse usando GET /auth/me", llamaremos a /me.
            await fetchUserData(data.accessToken);
            
            loginForm.reset();
        } catch (error) {
            errorMsg.textContent = error.message;
        } finally {
            btn.innerHTML = '<span>Iniciar Sesión</span><i class="fas fa-arrow-right"></i>';
            btn.disabled = false;
        }
    });

    document.getElementById('logout-btn').addEventListener('click', () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        dashboardScreen.classList.remove('active');
        loginScreen.classList.add('active');
        dynamicMenu.innerHTML = '';
    });

    async function fetchUserData(token) {
        try {
            const res = await fetch('/api/v1/auth/me', {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (!res.ok) {
                if (res.status === 401 || res.status === 403) {
                    throw new Error('Token expirado');
                }
                throw new Error('Error al cargar datos del usuario');
            }

            const data = await res.json();
            
            // Render profile info
            document.getElementById('display-username').textContent = data.user.username;
            document.getElementById('display-email').textContent = data.user.email || 'Sin correo';

            // Build Dynamic Menu from data.menus
            buildMenu(data.menus, dynamicMenu);
            
            // Show Dashboard
            loginScreen.classList.remove('active');
            dashboardScreen.classList.add('active');
            
        } catch (error) {
            console.error(error);
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            loginScreen.classList.add('active');
            dashboardScreen.classList.remove('active');
        }
    }

    function buildMenu(menus, container) {
        container.innerHTML = '';
        if (!menus || menus.length === 0) {
            container.innerHTML = '<li style="padding: 1rem; color: #94a3b8; font-size: 0.9rem;">No tienes menús asignados.</li>';
            return;
        }

        menus.forEach(menu => {
            const li = createMenuItem(menu);
            container.appendChild(li);
        });
    }

    function createMenuItem(menu) {
        const li = document.createElement('li');
        li.className = 'menu-item';
        
        const a = document.createElement('a');
        a.className = 'menu-link';
        
        // Icono (default cube)
        const iconClass = menu.icon ? (menu.icon.includes('fa-') ? menu.icon : `fas fa-${menu.icon}`) : 'fas fa-cube';
        
        let linkHTML = `<i class="${iconClass}"></i><span>${menu.label}</span>`;
        
        const hasChildren = menu.children && menu.children.length > 0;
        
        if (hasChildren) {
            linkHTML += `<i class="fas fa-chevron-right arrow"></i>`;
        }
        
        a.innerHTML = linkHTML;
        
        a.addEventListener('click', (e) => {
            if (hasChildren) {
                e.preventDefault();
                li.classList.toggle('open');
            } else {
                // Remove active class from all links
                document.querySelectorAll('.menu-link').forEach(link => link.classList.remove('active'));
                a.classList.add('active');
                
                // Update page view
                pageTitle.textContent = menu.label;
                renderContent(menu);
            }
        });
        
        li.appendChild(a);
        
        if (hasChildren) {
            const ul = document.createElement('ul');
            ul.className = 'submenu';
            menu.children.forEach(child => {
                ul.appendChild(createMenuItem(child));
            });
            li.appendChild(ul);
        }
        
        return li;
    }

    function renderContent(menu) {
        // En una app real esto enrutaría. Aquí simulamos el contenido de la ruta.
        mainContentArea.innerHTML = `
            <div class="glass-card fade-in">
                <h2><i class="${menu.icon ? (menu.icon.includes('fa-') ? menu.icon : 'fas fa-'+menu.icon) : 'fas fa-cube'}"></i> ${menu.label}</h2>
                <hr style="border-color: rgba(255,255,255,0.1); margin: 1rem 0;">
                <p style="color: #94a3b8; line-height: 1.6;">
                    Estás viendo el contenido del módulo <strong>${menu.label}</strong>.<br>
                    Ruta mapeada: <code>${menu.route || '/no-route'}</code><br>
                    Código de Menú: <code>${menu.code}</code>
                </p>
            </div>
        `;
    }
});
