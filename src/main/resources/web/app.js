document.addEventListener('DOMContentLoaded', () => {
    // ── BASE API URL (Soporte para http y file://) ──
    const API_BASE = (location.protocol === 'file:') ? 'http://localhost:8085' : '';

    // ── DOM ELEMENTS ──
    const txtSearch = document.getElementById('txtSearch');
    const btnSearch = document.getElementById('btnSearch');
    const formSearch = document.getElementById('formSearch');
    const btnClearSearchInput = document.getElementById('btnClearSearchInput');
    const btnClearSearchForm = document.getElementById('btnClearSearchForm');

    // Header & Sub-Header
    const btnOpenAsignarModal = document.getElementById('btnOpenAsignarModal');
    const btnReloadPage = document.getElementById('btnReloadPage');
    const selAlmacen = document.getElementById('selAlmacen'); // Eliminado del header
    const lblHeaderUser = document.getElementById('lblHeaderUser');

    // Modal Asignar Ubicación (Flujo Rápido estilo Sistema Viejo)
    const modalAsignarUbicacion = document.getElementById('modalAsignarUbicacion');
    const btnCloseAsignarModal = document.getElementById('btnCloseAsignarModal');
    const btnAsignarRegresar = document.getElementById('btnAsignarRegresar');
    const txtAsignarBarcode = document.getElementById('txtAsignarBarcode');
    const txtAsignarUbi = document.getElementById('txtAsignarUbi');
    const txtAsignarDesc = document.getElementById('txtAsignarDesc');
    const txtAsignarCodigo = document.getElementById('txtAsignarCodigo');
    const txtAsignarAlmacen = document.getElementById('txtAsignarAlmacen');
    const txtAsignarStock = document.getElementById('txtAsignarStock');
    const btnAsignarGuardar = document.getElementById('btnAsignarGuardar');
    const btnAsignarCameraBarcode = document.getElementById('btnAsignarCameraBarcode');
    const btnAsignarCameraUbi = document.getElementById('btnAsignarCameraUbi');
    const pnlAsignarUbiOcupada = document.getElementById('pnlAsignarUbiOcupada');
    const lblAsignarUbiOcupadaText = document.getElementById('lblAsignarUbiOcupadaText');

    // Elementos de Imagen en Modal Asignar
    const pnlModalImagePreview = document.getElementById('pnlModalImagePreview');
    const imgModalProduct = document.getElementById('imgModalProduct');
    const lblModalPreviewDesc = document.getElementById('lblModalPreviewDesc');
    const lblModalPreviewCode = document.getElementById('lblModalPreviewCode');
    const lblModalPreviewBarcode = document.getElementById('lblModalPreviewBarcode');
    const lblModalPreviewStock = document.getElementById('lblModalPreviewStock');
    const lblModalPreviewAveria = document.getElementById('lblModalPreviewAveria');
    const pnlModalAveriaWarn = document.getElementById('pnlModalAveriaWarn');
    const lblModalAveriaWarnText = document.getElementById('lblModalAveriaWarnText');

    let modalCurrentProduct = null;

    const btnScanCamera = document.getElementById('btnScanCamera');
    const btnScanUbi1Camera = document.getElementById('btnScanUbi1Camera');
    const btnScanUbi2Camera = document.getElementById('btnScanUbi2Camera');

    const modalCamera = document.getElementById('modalCamera');
    const btnCloseCamera = document.getElementById('btnCloseCamera');
    const btnCancelCamera = document.getElementById('btnCancelCamera');
    const lblCameraTargetTitle = document.getElementById('lblCameraTargetTitle');

    const pnlResultsList = document.getElementById('pnlResultsList');
    const lblResultsCount = document.getElementById('lblResultsCount');
    const resultsItemsWrapper = document.getElementById('resultsItemsWrapper');
    const btnCloseResults = document.getElementById('btnCloseResults');


    const lblCode = document.getElementById('lblCode');
    const lblBarcode = document.getElementById('lblBarcode');
    const lblDescription = document.getElementById('lblDescription');
    const lblBrandLine = document.getElementById('lblBrandLine');
    const lblStock = document.getElementById('lblStock');
    const pnlStockAveria = document.getElementById('pnlStockAveria');
    const lblStockAveria = document.getElementById('lblStockAveria');
    const pnlStockVencido = document.getElementById('pnlStockVencido');
    const lblStockVencido = document.getElementById('lblStockVencido');
    const pnlAveriaAlert = document.getElementById('pnlAveriaAlert');
    const lblAveriaAlertTitle = document.getElementById('lblAveriaAlertTitle');
    const lblAveriaAlertText = document.getElementById('lblAveriaAlertText');

    // Elementos de Imagen en Ficha Principal
    const imgProductDetail = document.getElementById('imgProductDetail');
    const lblImageStatus = document.getElementById('lblImageStatus');
    const lblImageStatusText = document.getElementById('lblImageStatusText');

    // Visualización de Ubicaciones en Ficha Principal (Modo Lectura Ultra Clara)
    const lblUbiActualDisplay = document.getElementById('lblUbiActualDisplay');
    const lblUbiAnteriorDisplay = document.getElementById('lblUbiAnteriorDisplay');
    const lblUbiStatus = document.getElementById('lblUbiStatus');
    const pnlUbiHeroCard = document.getElementById('pnlUbiHeroCard');
    const btnEditLocationMain = document.getElementById('btnEditLocationMain');
    const lblBtnEditLocationText = document.getElementById('lblBtnEditLocationText');

    const tblHistoryBody = document.getElementById('tblHistoryBody');
    const btnThemeToggle = document.getElementById('btnThemeToggle');
    const iconSun = document.getElementById('iconSun');
    const iconMoon = document.getElementById('iconMoon');
    const lblServerInfo = document.getElementById('lblServerInfo');

    // ── GESTIÓN DE CONFLICTOS (+1 PRODUCTO POR UBICACIÓN) ──
    const btnOpenConflictosModal = document.getElementById('btnOpenConflictosModal');
    const badgeConflictosCount = document.getElementById('badgeConflictosCount');
    const modalConflictos = document.getElementById('modalConflictos');
    const btnCloseConflictosModal = document.getElementById('btnCloseConflictosModal');
    const btnCloseConflictosFooter = document.getElementById('btnCloseConflictosFooter');
    const tabConflictosExcel = document.getElementById('tabConflictosExcel');
    const tabConflictosAll = document.getElementById('tabConflictosAll');
    const lblCountExcelTab = document.getElementById('lblCountExcelTab');
    const lblCountAllTab = document.getElementById('lblCountAllTab');
    const txtSearchConflictos = document.getElementById('txtSearchConflictos');
    const listConflictosUbis = document.getElementById('listConflictosUbis');
    const lblConflictoShelfName = document.getElementById('lblConflictoShelfName');
    const lblConflictoShelfBadge = document.getElementById('lblConflictoShelfBadge');
    const containerConflictoProductos = document.getElementById('containerConflictoProductos');

    // Alerta de conflicto en ficha principal
    const pnlConflictoDetectado = document.getElementById('pnlConflictoDetectado');
    const lblConflictoTitulo = document.getElementById('lblConflictoTitulo');
    const lblConflictoDetalle = document.getElementById('lblConflictoDetalle');
    const btnResolverConflictoDirecto = document.getElementById('btnResolverConflictoDirecto');

    let allConflictos = [];
    let conflictosByUbi = new Map();
    let currentConflictTab = 'excel';
    let selectedConflictUbi = null;

    // ── CAMBIO DE FOTO DEL PRODUCTO ──
    const btnCambiarFoto = document.getElementById('btnCambiarFoto');
    const modalCambiarFoto = document.getElementById('modalCambiarFoto');
    const btnCloseModalPhoto = document.getElementById('btnCloseModalPhoto');
    const btnCancelModalPhoto = document.getElementById('btnCancelModalPhoto');
    const btnSaveModalPhoto = document.getElementById('btnSaveModalPhoto');
    const txtPhotoUrl = document.getElementById('txtPhotoUrl');
    const btnPreviewPhotoUrl = document.getElementById('btnPreviewPhotoUrl');
    const filePhotoUpload = document.getElementById('filePhotoUpload');
    const imgNewPhotoPreview = document.getElementById('imgNewPhotoPreview');
    const lblModalPhotoSub = document.getElementById('lblModalPhotoSub');
    let uploadedPhotoBase64 = null;

    // ── STATE ──
    let currentProduct = null;
    let currentUser = 'ADMIN';
    let html5QrCode = null;
    let cameraScanTarget = 'search'; // 'search', 'ubi1', 'ubi2'
    let serverLocalIp = '172.16.0.71';
    let serverHttpPort = 8085;
    let serverHttpsPort = 8444;

    // ── FETCH SERVER INFO ──
    fetch(`${API_BASE}/api/info`)
        .then(res => res.json())
        .then(data => {
            if (data.usuario) {
                currentUser = data.usuario;
                if (data.localIp) serverLocalIp = data.localIp;
                if (data.httpPort) serverHttpPort = data.httpPort;
                if (data.httpsPort) serverHttpsPort = data.httpsPort;

                if (lblServerInfo) {
                    lblServerInfo.textContent = `Profit ${data.baseDatos || 'DROA_A'}`;
                    lblServerInfo.title = `Conectado a ${data.baseDatos} en ${data.maquina} | IP: ${serverLocalIp} | Usuario: ${data.usuario} (${data.nombreUsuario})`;
                }
                if (lblHeaderUser) {
                    lblHeaderUser.textContent = `Usuario: ${data.usuario}`;
                }
            }
        })
        .catch(() => { });

    // ── EVENT LISTENERS (HEADER & GENERAL) ──
    if (btnReloadPage) {
        btnReloadPage.addEventListener('click', () => {
            clearForm();
            txtSearch.value = '';
            txtSearch.focus();
            showToast('Pantalla reiniciada', 'info');
        });
    }

    if (btnOpenAsignarModal) {
        btnOpenAsignarModal.addEventListener('click', openAsignarModal);
    }
    if (btnCloseAsignarModal) {
        btnCloseAsignarModal.addEventListener('click', closeAsignarModal);
    }
    if (btnAsignarRegresar) {
        btnAsignarRegresar.addEventListener('click', closeAsignarModal);
    }

    // Limpieza rápida de buscador principal
    if (txtSearch && btnClearSearchInput) {
        txtSearch.addEventListener('input', () => {
            btnClearSearchInput.classList.toggle('hidden', !txtSearch.value.trim());
        });
        btnClearSearchInput.addEventListener('click', () => {
            txtSearch.value = '';
            btnClearSearchInput.classList.add('hidden');
            clearForm();
            txtSearch.focus();
        });
    }

    if (btnClearSearchForm) {
        btnClearSearchForm.addEventListener('click', () => {
            txtSearch.value = '';
            if (btnClearSearchInput) btnClearSearchInput.classList.add('hidden');
            clearForm();
            txtSearch.focus();
        });
    }

    // Atajos de Teclado Globales: F2 abre Asignar Ubicación, Esc lo cierra
    window.addEventListener('keydown', (e) => {
        if (e.key === 'F2') {
            e.preventDefault();
            if (modalAsignarUbicacion.classList.contains('hidden')) {
                if (currentProduct) {
                    openAsignarModalWithProduct(currentProduct);
                } else {
                    openAsignarModal();
                }
            } else {
                closeAsignarModal();
            }
        } else if (e.key === 'Escape') {
            if (!modalAsignarUbicacion.classList.contains('hidden')) {
                closeAsignarModal();
            }
        }
    });

    // ── EVENT LISTENERS (BUSCADOR PRINCIPAL) ──
    formSearch.addEventListener('submit', (e) => {
        e.preventDefault();
        searchProduct();
    });

    btnSearch.addEventListener('click', searchProduct);
    if (btnEditLocationMain) {
        btnEditLocationMain.addEventListener('click', () => {
            if (currentProduct) {
                openAsignarModalWithProduct(currentProduct);
            } else {
                openAsignarModal();
            }
        });
    }
    if (btnCloseResults) btnCloseResults.addEventListener('click', hideResultsList);

    // Cámara Triggers
    if (btnScanCamera) btnScanCamera.addEventListener('click', () => startCameraScanner('search'));

    // Cámara Triggers en Modal Asignar Ubicación
    if (btnAsignarCameraBarcode) btnAsignarCameraBarcode.addEventListener('click', () => startCameraScanner('modalBarcode'));
    if (btnAsignarCameraUbi) btnAsignarCameraUbi.addEventListener('click', () => startCameraScanner('modalUbi'));

    // Eventos en Modal Asignar Ubicación
    if (txtAsignarBarcode) {
        txtAsignarBarcode.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                lookupProductForModal();
            }
        });
        // Si el usuario pega o escanea y pierde el foco
        txtAsignarBarcode.addEventListener('change', () => {
            if (!modalCurrentProduct && txtAsignarBarcode.value.trim()) {
                lookupProductForModal();
            }
        });
    }

    if (txtAsignarUbi) {
        txtAsignarUbi.addEventListener('input', () => {
            checkAsignarModalUbiOcupada();
        });
        txtAsignarUbi.addEventListener('blur', () => {
            checkAsignarModalUbiOcupada();
        });
        txtAsignarUbi.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                if (!btnAsignarGuardar.disabled) {
                    saveAsignarModal();
                }
            }
        });
    }

    if (btnAsignarGuardar) {
        btnAsignarGuardar.addEventListener('click', saveAsignarModal);
    }

    btnCloseCamera.addEventListener('click', stopCameraScanner);
    btnCancelCamera.addEventListener('click', stopCameraScanner);

    // Theme Switcher
    btnThemeToggle.addEventListener('click', () => {
        const html = document.documentElement;
        const isDark = html.getAttribute('data-theme') === 'dark';
        const newTheme = isDark ? 'light' : 'dark';
        html.setAttribute('data-theme', newTheme);

        iconSun.classList.toggle('hidden', !isDark);
        iconMoon.classList.toggle('hidden', isDark);
    });

    const fileCameraInput = document.getElementById('fileCameraInput');

    if (fileCameraInput) {
        fileCameraInput.addEventListener('change', async (e) => {
            if (!e.target.files || e.target.files.length === 0) return;
            const file = e.target.files[0];
            showToast('⏳ Escaneando foto de cámara...', 'info');

            try {
                if (!html5QrCode) {
                    html5QrCode = new Html5Qrcode("cameraReader");
                }
                const decodedText = await html5QrCode.scanFile(file, true);
                onCameraScanSuccess(decodedText);
            } catch (err) {
                showToast('❌ No se pudo detectar un código en la foto. Intente tomarla más cerca y enfocada.', 'error');
            } finally {
                fileCameraInput.value = '';
            }
        });
    }

    // ── CAMERA SCANNER FUNCTIONS ──

    /** Espera a que la librería html5-qrcode termine de cargar (defer). */
    function waitForQrLib(maxMs = 5000) {
        return new Promise((resolve, reject) => {
            if (typeof Html5Qrcode !== 'undefined') { resolve(); return; }
            const t0 = Date.now();
            const interval = setInterval(() => {
                if (typeof Html5Qrcode !== 'undefined') { clearInterval(interval); resolve(); }
                else if (Date.now() - t0 > maxMs) { clearInterval(interval); reject(new Error('timeout')); }
            }, 100);
        });
    }

    async function startCameraScanner(target) {
        cameraScanTarget = target;

        if (target === 'search') {
            lblCameraTargetTitle.textContent = '📷 Escanear Producto o Pasillo';
        } else if (target === 'modalBarcode') {
            lblCameraTargetTitle.textContent = '📷 Escanear Código de Barra del Producto';
        } else if (target === 'modalUbi') {
            lblCameraTargetTitle.textContent = '📍 Escanear Etiqueta de Estante';
        }

        // Esperar a que la librería QR cargue (puede estar en defer)
        try {
            await waitForQrLib();
        } catch (_) {
            showToast('❌ La librería de cámara no se ha cargado. Verifique su conexión a Internet.', 'error');
            return;
        }

        // Verificar si el navegador móvil permite streaming de video por HTTP sin HTTPS
        const hasLiveStreaming = navigator.mediaDevices && navigator.mediaDevices.getUserMedia && (location.protocol === 'https:' || location.hostname === 'localhost' || location.hostname === '127.0.0.1');

        if (!hasLiveStreaming && fileCameraInput) {
            // En HTTP sin SSL, los navegadores móviles bloquean el video en vivo. Abrimos la Cámara Nativa del smartphone.
            showToast('📷 Abriendo Cámara Nativa del Teléfono...', 'info');
            fileCameraInput.click();
            return;
        }

        modalCamera.classList.remove('hidden');

        try {
            if (!html5QrCode) {
                html5QrCode = new Html5Qrcode("cameraReader");
            }

            const config = {
                fps: 15,
                qrbox: { width: 260, height: 160 },
                aspectRatio: 1.0
            };

            await html5QrCode.start(
                { facingMode: "environment" },
                config,
                onCameraScanSuccess,
                () => { } // Ignorar fallos de frames individuales
            );
        } catch (err) {
            modalCamera.classList.add('hidden');
            // Si el streaming en vivo falla en el navegador, fallback automático a Cámara Nativa
            if (fileCameraInput) {
                showToast('📷 Abriendo Cámara del Teléfono...', 'info');
                fileCameraInput.click();
            } else {
                showToast(`❌ Error al acceder a la cámara: ${err.message || err}`, 'error');
            }
        }
    }


    function stopCameraScanner() {
        if (html5QrCode && html5QrCode.isScanning) {
            html5QrCode.stop().then(() => {
                modalCamera.classList.add('hidden');
            }).catch(() => {
                modalCamera.classList.add('hidden');
            });
        } else {
            modalCamera.classList.add('hidden');
        }
    }

    function onCameraScanSuccess(decodedText) {
        playBeep();
        if (navigator.vibrate) {
            navigator.vibrate(100);
        }

        stopCameraScanner();

        const code = decodedText.trim();
        showToast(`✔ Leído por cámara: ${code}`, 'success');

        if (cameraScanTarget === 'search') {
            txtSearch.value = code;
            searchProduct();
        } else if (cameraScanTarget === 'modalBarcode') {
            txtAsignarBarcode.value = code;
            lookupProductForModal(code);
        } else if (cameraScanTarget === 'modalUbi') {
            txtAsignarUbi.value = code;
            checkAsignarModalUbiOcupada();
            txtAsignarUbi.focus();
        }
    }

    function playBeep() {
        try {
            const ctx = new (window.AudioContext || window.webkitAudioContext)();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'sine';
            osc.frequency.value = 1200;
            gain.gain.value = 0.3;
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start();
            osc.stop(ctx.currentTime + 0.12);
        } catch (_) { }
    }

    // ── SEARCH FUNCTIONS ──
    async function searchProduct() {
        const query = txtSearch.value.trim();
        if (!query) {
            showToast('Ingrese Nombre de producto, Código Interno, Barras o Pasillo para buscar', 'warning');
            txtSearch.focus();
            return;
        }

        // En pantallas móviles, ocultar teclado virtual para ver los resultados de inmediato
        if (window.innerWidth <= 768 && txtSearch) {
            txtSearch.blur();
        }

        btnSearch.disabled = true;
        txtSearch.disabled = true;
        hideResultsList();

        try {
            const response = await fetch(`${API_BASE}/api/articulo?q=${encodeURIComponent(query)}`);
            if (response.ok) {
                const results = await response.json();
                if (Array.isArray(results) && results.length > 1) {
                    displayMultipleResults(results);
                    showToast(`🔍 Se encontraron ${results.length} coincidencias`, 'info');
                } else if (Array.isArray(results) && results.length === 1) {
                    loadProductData(results[0]);
                    showToast(`✔ Producto encontrado: ${results[0].coArt}`, 'success');
                    if (btnEditLocationMain) btnEditLocationMain.focus();
                } else {
                    showToast('❌ No se encontraron productos con ese criterio', 'error');
                    clearForm();
                    txtSearch.focus();
                    txtSearch.select();
                }
            } else {
                const error = await response.json();
                showToast(`❌ ${error.error || 'Producto no encontrado'}`, 'error');
                clearForm();
                txtSearch.focus();
                txtSearch.select();
            }
        } catch (err) {
            showToast(`❌ Error al conectar con el servidor: ${err.message}`, 'error');
            txtSearch.focus();
        } finally {
            btnSearch.disabled = false;
            txtSearch.disabled = false;
        }
    }

    function displayMultipleResults(results) {
        lblResultsCount.textContent = `Resultados encontrados (${results.length}) - Seleccione el producto:`;
        resultsItemsWrapper.innerHTML = '';

        results.forEach(product => {
            const item = document.createElement('div');
            item.className = 'result-card-item';

            const ubiAct = product.ubicacion1 ? `📍 Actual: ${product.ubicacion1}` : '📍 Actual: Sin asignar';
            const ubiAnt = product.ubicacion2 ? `⏳ Estuvo en: ${product.ubicacion2}` : '';
            const imgUrl = product.imagenUrl || `${API_BASE}/api/imagen?id=${encodeURIComponent(product.coArt)}`;

            item.innerHTML = `
                <div class="result-item-thumb-box">
                    <img src="${imgUrl}" class="result-item-img" alt="" loading="lazy" onerror="this.src='${API_BASE}/api/imagen?id=placeholder';">
                </div>
                <div class="result-item-info">
                    <span class="result-item-title">${escapeHtml(product.descripcion)}</span>
                    <div class="result-item-sub">
                        <span>Cód: <strong>${escapeHtml(product.coArt)}</strong></span>
                        ${product.codigoBarra ? `<span>Ref: ${escapeHtml(product.codigoBarra)}</span>` : ''}
                        <span>Stock Venta: <strong style="color: var(--accent-green);">${Number(product.existencia || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 })}</strong></span>
                        ${Number(product.stockAveria || 0) > 0 ? `<span class="badge-averia" style="font-size: 0.72rem; padding: 2px 6px;">⚠️ Av: ${Number(product.stockAveria).toLocaleString('es-ES', { minimumFractionDigits: 0 })}</span>` : ''}
                    </div>
                    <div class="result-item-ubis">
                        <span class="badge-ubi-act">${escapeHtml(ubiAct)}</span>
                        ${ubiAnt ? `<span class="badge-ubi-ant">${escapeHtml(ubiAnt)}</span>` : ''}
                    </div>
                </div>
            `;

            item.addEventListener('click', () => {
                loadProductData(product);
                hideResultsList();
                showToast(`✔ Producto seleccionado: ${product.coArt}`, 'success');
                if (btnEditLocationMain) btnEditLocationMain.focus();
            });

            resultsItemsWrapper.appendChild(item);
        });

        pnlResultsList.classList.remove('hidden');
    }

    function hideResultsList() {
        pnlResultsList.classList.add('hidden');
    }

    function loadProductData(product) {
        currentProduct = product;
        lblCode.textContent = `Código: ${product.coArt}`;
        lblBarcode.textContent = `Barras: ${product.codigoBarra || '---'}`;
        lblDescription.textContent = product.descripcion;
        lblBrandLine.textContent = `${product.marca || 'N/A'} / ${product.linea || 'N/A'}`;

        const stDisp = Number(product.existencia || 0);
        const stAveria = Number(product.stockAveria || 0);
        const stVenc = Number(product.stockVencido || 0);

        lblStock.textContent = stDisp.toLocaleString('es-ES', { minimumFractionDigits: 2 });

        // Stock en Averías
        if (pnlStockAveria && lblStockAveria) {
            if (stAveria > 0) {
                pnlStockAveria.classList.remove('hidden');
                lblStockAveria.textContent = stAveria.toLocaleString('es-ES', { minimumFractionDigits: 2 });
            } else {
                pnlStockAveria.classList.add('hidden');
            }
        }

        // Stock Vencido
        if (pnlStockVencido && lblStockVencido) {
            if (stVenc > 0) {
                pnlStockVencido.classList.remove('hidden');
                lblStockVencido.textContent = stVenc.toLocaleString('es-ES', { minimumFractionDigits: 2 });
            } else {
                pnlStockVencido.classList.add('hidden');
            }
        }

        // Alerta Destacada de Averías
        if (pnlAveriaAlert) {
            if (stAveria > 0) {
                pnlAveriaAlert.classList.remove('hidden');
                if (stDisp <= 0) {
                    if (lblAveriaAlertTitle) lblAveriaAlertTitle.textContent = '🚨 Stock SÓLO en Averías (No Apto para Venta)';
                    if (lblAveriaAlertText) lblAveriaAlertText.textContent = `Este producto no tiene existencia disponible para venta comercial. Posee ${stAveria.toLocaleString('es-ES', { minimumFractionDigits: 0 })} unidad(es) apartada(s) en el Almacén de Averías (AV). Si busca en el estante de despacho, estará vacío.`;
                } else {
                    if (lblAveriaAlertTitle) lblAveriaAlertTitle.textContent = '⚠️ Mercancía con Unidades en Averías (AV)';
                    if (lblAveriaAlertText) lblAveriaAlertText.textContent = `Atención: Este producto tiene ${stAveria.toLocaleString('es-ES', { minimumFractionDigits: 0 })} unidad(es) averiada(s) o dañada(s) en Almacén AV, además de las ${stDisp.toLocaleString('es-ES', { minimumFractionDigits: 0 })} unidades aptas para la venta.`;
                }
            } else {
                pnlAveriaAlert.classList.add('hidden');
            }
        }

        // Cargar imagen del producto
        if (imgProductDetail) {
            let imgUrl = product.imagenUrl || `${API_BASE}/api/imagen?id=${encodeURIComponent(product.coArt)}`;
            if (!imgUrl.includes('&v=') && !imgUrl.includes('?v=')) {
                imgUrl += (imgUrl.includes('?') ? '&' : '?') + 't=' + Date.now();
            }
            imgProductDetail.src = imgUrl;
            imgProductDetail.onload = () => {
                if (lblImageStatus) lblImageStatus.classList.remove('hidden');
            };
            imgProductDetail.onerror = () => {
                if (lblImageStatus) lblImageStatus.classList.add('hidden');
            };
        }

        // Ubicación en Almacén (Modo Visualización Destacada)
        if (lblUbiActualDisplay) {
            if (product.ubicacion1 && product.ubicacion1.trim()) {
                lblUbiActualDisplay.textContent = product.ubicacion1.trim();
                if (pnlUbiHeroCard) {
                    pnlUbiHeroCard.classList.remove('no-location');
                    pnlUbiHeroCard.classList.add('has-location');
                }
                if (lblUbiStatus) lblUbiStatus.textContent = '📍 Estante asignado en almacén';
                if (lblBtnEditLocationText) lblBtnEditLocationText.textContent = 'Modificar en "Asignar Ubicación" (F2)';
            } else {
                lblUbiActualDisplay.textContent = 'Sin Asignar';
                if (pnlUbiHeroCard) {
                    pnlUbiHeroCard.classList.remove('has-location');
                    pnlUbiHeroCard.classList.add('no-location');
                }
                if (lblUbiStatus) lblUbiStatus.textContent = '⚠️ Este producto no tiene estante asignado';
                if (lblBtnEditLocationText) lblBtnEditLocationText.textContent = 'Asignar Estante a este Producto (F2)';
            }
        }

        if (lblUbiAnteriorDisplay) {
            lblUbiAnteriorDisplay.textContent = (product.ubicacion2 && product.ubicacion2.trim()) ? product.ubicacion2.trim() : 'Ninguna (Sin historial)';
        }

        // Verificar si la ubicación actual de este producto está en conflicto (+1 productos)
        if (pnlConflictoDetectado) {
            const ubiAct = (product.ubicacion1 || '').trim().toUpperCase();
            if (ubiAct && conflictosByUbi.has(ubiAct)) {
                const conf = conflictosByUbi.get(ubiAct);
                pnlConflictoDetectado.classList.remove('hidden');
                if (lblConflictoTitulo) {
                    lblConflictoTitulo.textContent = `¡Estante ${ubiAct} con conflicto (${conf.cantidad} productos)!`;
                }
                if (lblConflictoDetalle) {
                    lblConflictoDetalle.textContent = `Hay ${conf.cantidad} productos asignados a este mismo estante en Profit Plus.`;
                }
            } else {
                pnlConflictoDetectado.classList.add('hidden');
            }
        }

        if (btnEditLocationMain) {
            btnEditLocationMain.disabled = false;
        }
    }

    function clearForm() {
        currentProduct = null;
        lblCode.textContent = 'Código: ---';
        lblBarcode.textContent = 'Barras: ---';
        lblDescription.textContent = 'Escanee o busque un producto o pasillo para comenzar...';
        lblBrandLine.textContent = '---';
        lblStock.textContent = '0.00';
        if (pnlStockAveria) pnlStockAveria.classList.add('hidden');
        if (pnlStockVencido) pnlStockVencido.classList.add('hidden');
        if (pnlAveriaAlert) pnlAveriaAlert.classList.add('hidden');

        if (imgProductDetail) {
            imgProductDetail.src = `${API_BASE}/api/imagen?id=placeholder`;
        }
        if (lblImageStatus) {
            lblImageStatus.classList.add('hidden');
        }

        if (lblUbiActualDisplay) {
            lblUbiActualDisplay.textContent = '---';
        }
        if (pnlUbiHeroCard) {
            pnlUbiHeroCard.classList.remove('has-location', 'no-location');
        }
        if (lblUbiStatus) {
            lblUbiStatus.textContent = 'Consulte o escanee un producto';
        }
        if (lblUbiAnteriorDisplay) {
            lblUbiAnteriorDisplay.textContent = '---';
        }
        if (btnEditLocationMain) {
            btnEditLocationMain.disabled = true;
        }
        if (lblBtnEditLocationText) {
            lblBtnEditLocationText.textContent = 'Modificar en "Asignar Ubicación" (F2)';
        }
        if (pnlConflictoDetectado) {
            pnlConflictoDetectado.classList.add('hidden');
        }
        hideResultsList();
    }

    function addHistoryRow(code, description, u1, u2) {
        const emptyRow = tblHistoryBody.querySelector('.empty-row');
        if (emptyRow) {
            emptyRow.remove();
        }

        const now = new Date();
        const timeStr = now.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${timeStr}</strong></td>
            <td><code>${escapeHtml(code)}</code></td>
            <td>${escapeHtml(description)}</td>
            <td><span class="badge badge-code">${escapeHtml(u1 || '---')}</span></td>
            <td><span class="badge badge-code">${escapeHtml(u2 || '---')}</span></td>
            <td>${escapeHtml(currentUser)}</td>
        `;

        tblHistoryBody.insertBefore(tr, tblHistoryBody.firstChild);
    }

    function showToast(message, type = 'info') {
        const container = document.getElementById('toastContainer');
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.textContent = message;

        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(20px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3500);
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    }

    // ── MODAL ASIGNAR UBICACION: FUNCIONES Y LÓGICA ──
    function openAsignarModal() {
        if (!modalAsignarUbicacion) return;
        modalAsignarUbicacion.classList.remove('hidden');
        clearModalFields(true);
        setTimeout(() => {
            if (txtAsignarBarcode) {
                txtAsignarBarcode.focus();
                txtAsignarBarcode.select();
            }
        }, 80);
    }

    function openAsignarModalWithProduct(prod) {
        if (!modalAsignarUbicacion || !prod) return;
        modalAsignarUbicacion.classList.remove('hidden');
        modalCurrentProduct = prod;

        if (txtAsignarBarcode) txtAsignarBarcode.value = prod.codigoBarra || prod.coArt || '';
        if (txtAsignarDesc) txtAsignarDesc.value = prod.descripcion || '';
        if (txtAsignarCodigo) txtAsignarCodigo.value = prod.coArt || '';
        if (txtAsignarStock) txtAsignarStock.value = Number(prod.existencia || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 });
        if (txtAsignarAlmacen) txtAsignarAlmacen.value = '01';

        if (pnlModalImagePreview) {
            pnlModalImagePreview.classList.remove('hidden');
            if (imgModalProduct) {
                imgModalProduct.src = prod.imagenUrl || `${API_BASE}/api/imagen?id=${encodeURIComponent(prod.coArt)}`;
            }
            if (lblModalPreviewDesc) lblModalPreviewDesc.textContent = prod.descripcion || '';
            if (lblModalPreviewCode) lblModalPreviewCode.textContent = `CÓD: ${prod.coArt || '---'}`;
            if (lblModalPreviewBarcode) lblModalPreviewBarcode.textContent = `BARRAS: ${prod.codigoBarra || '---'}`;
            if (lblModalPreviewStock) lblModalPreviewStock.textContent = `STOCK: ${Number(prod.existencia || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 })}`;

            const stAv = Number(prod.stockAveria || 0);
            if (lblModalPreviewAveria) {
                if (stAv > 0) {
                    lblModalPreviewAveria.classList.remove('hidden');
                    lblModalPreviewAveria.textContent = `⚠️ AVERÍAS: ${stAv.toLocaleString('es-ES', { minimumFractionDigits: 0 })}`;
                } else {
                    lblModalPreviewAveria.classList.add('hidden');
                }
            }
        }

        if (pnlModalAveriaWarn) {
            const stAv = Number(prod.stockAveria || 0);
            if (stAv > 0) {
                pnlModalAveriaWarn.classList.remove('hidden');
                if (lblModalAveriaWarnText) {
                    lblModalAveriaWarnText.textContent = `Atención: Este producto tiene ${stAv.toLocaleString('es-ES', { minimumFractionDigits: 0 })} unidad(es) en Almacén de Averías (AV). Verifique físicamente que no esté colocando mercancía dañada en estantes de venta.`;
                }
            } else {
                pnlModalAveriaWarn.classList.add('hidden');
            }
        }

        if (txtAsignarUbi) {
            txtAsignarUbi.value = prod.ubicacion1 || '';
            checkAsignarModalUbiOcupada();
            setTimeout(() => {
                txtAsignarUbi.focus();
                txtAsignarUbi.select();
            }, 80);
        }

        if (btnAsignarGuardar) btnAsignarGuardar.disabled = false;
    }

    function closeAsignarModal() {
        if (!modalAsignarUbicacion) return;
        modalAsignarUbicacion.classList.add('hidden');
        if (txtSearch) txtSearch.focus();
    }

    function clearModalFields(clearBarcode = true) {
        modalCurrentProduct = null;
        if (clearBarcode && txtAsignarBarcode) {
            txtAsignarBarcode.value = '';
        }
        if (txtAsignarUbi) txtAsignarUbi.value = '';
        if (txtAsignarDesc) txtAsignarDesc.value = '';
        if (txtAsignarCodigo) txtAsignarCodigo.value = '';
        if (txtAsignarStock) txtAsignarStock.value = '0.00';
            if (txtAsignarAlmacen) {
                txtAsignarAlmacen.value = '01';
            }

            // Limpiar vista previa de imagen en modal
            if (pnlModalImagePreview) pnlModalImagePreview.classList.add('hidden');
            if (lblModalPreviewAveria) lblModalPreviewAveria.classList.add('hidden');
            if (pnlModalAveriaWarn) pnlModalAveriaWarn.classList.add('hidden');
            if (imgModalProduct) imgModalProduct.src = `${API_BASE}/api/imagen?id=placeholder`;

            if (pnlAsignarUbiOcupada) pnlAsignarUbiOcupada.classList.add('hidden');
            if (btnAsignarGuardar) btnAsignarGuardar.disabled = true;
        }

        async function lookupProductForModal(codeOverride) {
            const q = (codeOverride || (txtAsignarBarcode ? txtAsignarBarcode.value : '')).trim();
            if (!q) {
                showToast('Escanee o ingrese el código de barras o código de artículo', 'warning');
                if (txtAsignarBarcode) txtAsignarBarcode.focus();
                return;
            }

            try {
                const response = await fetch(`${API_BASE}/api/articulo?q=${encodeURIComponent(q)}`);
                if (response.ok) {
                    const results = await response.json();
                    if (Array.isArray(results) && results.length > 0) {
                        // Si hay varios, priorizar coincidencia exacta de código o código de barras
                        let prod = results.find(r => r.coArt.trim().toUpperCase() === q.toUpperCase() ||
                            (r.codigoBarra && r.codigoBarra.trim().toUpperCase() === q.toUpperCase()));
                        if (!prod) prod = results[0];

                        modalCurrentProduct = prod;
                        if (txtAsignarDesc) txtAsignarDesc.value = prod.descripcion || '';
                        if (txtAsignarCodigo) txtAsignarCodigo.value = prod.coArt || '';
                        if (txtAsignarStock) {
                            txtAsignarStock.value = Number(prod.existencia || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 });
                        }
                            if (txtAsignarAlmacen) {
                                txtAsignarAlmacen.value = '01';
                            }

                            // Cargar imagen en modal
                            if (pnlModalImagePreview) {
                                pnlModalImagePreview.classList.remove('hidden');
                                if (imgModalProduct) {
                                    imgModalProduct.src = prod.imagenUrl || `${API_BASE}/api/imagen?id=${encodeURIComponent(prod.coArt)}`;
                                }
                                if (lblModalPreviewDesc) lblModalPreviewDesc.textContent = prod.descripcion || '';
                                if (lblModalPreviewCode) lblModalPreviewCode.textContent = `CÓD: ${prod.coArt || '---'}`;
                                if (lblModalPreviewBarcode) lblModalPreviewBarcode.textContent = `BARRAS: ${prod.codigoBarra || '---'}`;
                                if (lblModalPreviewStock) lblModalPreviewStock.textContent = `STOCK: ${Number(prod.existencia || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 })}`;

                                const stAv = Number(prod.stockAveria || 0);
                                if (lblModalPreviewAveria) {
                                    if (stAv > 0) {
                                        lblModalPreviewAveria.classList.remove('hidden');
                                        lblModalPreviewAveria.textContent = `⚠️ AVERÍAS: ${stAv.toLocaleString('es-ES', { minimumFractionDigits: 0 })}`;
                                    } else {
                                        lblModalPreviewAveria.classList.add('hidden');
                                    }
                                }
                            }

                            if (pnlModalAveriaWarn) {
                                const stAv = Number(prod.stockAveria || 0);
                                if (stAv > 0) {
                                    pnlModalAveriaWarn.classList.remove('hidden');
                                    if (lblModalAveriaWarnText) {
                                        lblModalAveriaWarnText.textContent = `Atención: Este producto tiene ${stAv.toLocaleString('es-ES', { minimumFractionDigits: 0 })} unidad(es) en Almacén de Averías (AV). Verifique físicamente que no esté colocando mercancía dañada en estantes de venta.`;
                                    }
                                } else {
                                    pnlModalAveriaWarn.classList.add('hidden');
                                }
                            }

                            // Foco directo en Ubicación
                            if (txtAsignarUbi) {
                                txtAsignarUbi.value = prod.ubicacion1 || '';
                                checkAsignarModalUbiOcupada();
                                setTimeout(() => {
                                    txtAsignarUbi.focus();
                                    txtAsignarUbi.select();
                                }, 50);
                            }

                            if (btnAsignarGuardar) btnAsignarGuardar.disabled = false;
                            playBeep();
                            showToast(`✔ Cargado: [${prod.coArt}] ${prod.descripcion}`, 'success');
                        } else {
                            showToast('❌ Producto no encontrado con ese código', 'error');
                            clearModalFields(false);
                            if (txtAsignarBarcode) {
                                txtAsignarBarcode.focus();
                                txtAsignarBarcode.select();
                            }
                        }
                    } else {
                        showToast('❌ Producto no encontrado en Profit Plus', 'error');
                        clearModalFields(false);
                        if (txtAsignarBarcode) {
                            txtAsignarBarcode.focus();
                            txtAsignarBarcode.select();
                        }
                    }
                } catch (err) {
                    showToast(`❌ Error al consultar producto: ${err.message}`, 'error');
                }
            }

    let modalUbiCheckTimeout = null;
            function checkAsignarModalUbiOcupada() {
                if (!txtAsignarUbi || !pnlAsignarUbiOcupada) return;
                if (modalUbiCheckTimeout) clearTimeout(modalUbiCheckTimeout);
                modalUbiCheckTimeout = setTimeout(async () => {
                    const ubi = txtAsignarUbi.value.trim().toUpperCase();
                    if (!ubi || !modalCurrentProduct) {
                        pnlAsignarUbiOcupada.classList.add('hidden');
                        return;
                    }

                    // Si es la misma ubicación actual que ya tiene este producto, no hay conflicto
                    if (modalCurrentProduct.ubicacion1 && ubi === modalCurrentProduct.ubicacion1.toUpperCase()) {
                        pnlAsignarUbiOcupada.classList.add('hidden');
                        return;
                    }

                    try {
                        const res = await fetch(`${API_BASE}/api/ubicacion/check?ubi=${encodeURIComponent(ubi)}&coArt=${encodeURIComponent(modalCurrentProduct.coArt)}`);
                        if (res.ok) {
                            const data = await res.json();
                            if (data.ocupado && data.articulos && data.articulos.length > 0) {
                                const artsText = data.articulos.map(a => `[${a.coArt}] ${a.descripcion}`).join(' • ');
                                lblAsignarUbiOcupadaText.innerHTML = `Estante ocupado por: <strong>${escapeHtml(artsText)}</strong>`;
                                pnlAsignarUbiOcupada.classList.remove('hidden');
                            } else {
                                pnlAsignarUbiOcupada.classList.add('hidden');
                            }
                        } else {
                            pnlAsignarUbiOcupada.classList.add('hidden');
                        }
                    } catch (_) {
                        pnlAsignarUbiOcupada.classList.add('hidden');
                    }
                }, 180);
            }

            async function saveAsignarModal() {
                if (!modalCurrentProduct) {
                    showToast('Escanee o busque un producto primero', 'warning');
                    if (txtAsignarBarcode) txtAsignarBarcode.focus();
                    return;
                }

                const ubi = txtAsignarUbi ? txtAsignarUbi.value.trim().toUpperCase() : '';
                if (btnAsignarGuardar) btnAsignarGuardar.disabled = true;

                try {
                    const response = await fetch(`${API_BASE}/api/ubicaciones`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            coArt: modalCurrentProduct.coArt,
                            ubicacion1: ubi,
                            ubicacion2: modalCurrentProduct.ubicacion1 || ''
                        })
                    });

                    const result = await response.json();
                    if (response.ok && result.success) {
                        playBeep();
                        if (navigator.vibrate) navigator.vibrate(150);

                        if (result.desalojados && result.desalojados.length > 0) {
                            const displaced = result.desalojados.map(d => `[${d.coArt}] ${d.descripcion}`).join(' • ');
                            showToast(`✅ Ubicación ${ubi} guardada. Se liberó el estante del producto anterior: ${displaced}`, 'success');
                        } else {
                            showToast(`✅ Ubicación ${ubi || '(sin ubicación)'} guardada correctamente en Profit Plus`, 'success');
                        }

                        // Registrar en historial de la sesión
                        addHistoryRow(modalCurrentProduct.coArt, modalCurrentProduct.descripcion, ubi, modalCurrentProduct.ubicacion1 || '');

                        // Actualizar inmediatamente la visualización en la ficha principal si corresponde
                        if (currentProduct && currentProduct.coArt === modalCurrentProduct.coArt) {
                            currentProduct.ubicacion2 = modalCurrentProduct.ubicacion1 || '';
                            currentProduct.ubicacion1 = ubi;
                            loadProductData(currentProduct);
                        }

                        // Ciclo ultrarrápido: Limpiar modal y enfocar inmediatamente en CODIGO DE BARRA PROD para el siguiente producto!
                        clearModalFields(true);
                        if (txtAsignarBarcode) {
                            txtAsignarBarcode.focus();
                        }
                    } else {
                        if (btnAsignarGuardar) btnAsignarGuardar.disabled = false;
                        showToast(`❌ ${result.error || 'No se pudo guardar la ubicación'}`, 'error');
                    }
                } catch (err) {
                    if (btnAsignarGuardar) btnAsignarGuardar.disabled = false;
                    showToast(`❌ Error de conexión: ${err.message}`, 'error');
                }
            }

            // ── MÓDULO AUDITOR Y RESOLUCIÓN DE CONFLICTOS (+1 PRODUCTO) ──
            async function loadConflictos(selectUbi = null) {
                try {
                    const res = await fetch(`${API_BASE}/api/conflictos`);
                    if (res.ok) {
                        const data = await res.json();
                        allConflictos = data.conflictos || [];
                        conflictosByUbi.clear();
                        allConflictos.forEach(c => {
                            conflictosByUbi.set(c.ubicacion.toUpperCase(), c);
                        });

                        if (badgeConflictosCount) {
                            badgeConflictosCount.textContent = data.excelCount > 0 ? `${data.excelCount} / ${data.total}` : `${data.total}`;
                        }
                        const navConflictosBadge = document.getElementById('navConflictosBadge');
                        if (navConflictosBadge) {
                            const cnt = data.excelCount > 0 ? data.excelCount : data.total;
                            navConflictosBadge.textContent = cnt;
                            if (cnt > 0) {
                                navConflictosBadge.classList.remove('hidden');
                            } else {
                                navConflictosBadge.classList.add('hidden');
                            }
                        }
                        if (lblCountExcelTab) lblCountExcelTab.textContent = data.excelCount || 0;
                        if (lblCountAllTab) lblCountAllTab.textContent = data.total || 0;

                        renderConflictSidebar();

                        if (selectUbi && conflictosByUbi.has(selectUbi.toUpperCase())) {
                            selectConflictUbi(selectUbi.toUpperCase());
                        } else if (allConflictos.length > 0 && !selectedConflictUbi) {
                            const first = getFilteredConflictos()[0];
                            if (first) selectConflictUbi(first.ubicacion);
                        }
                    }
                } catch (err) {
                    console.error('Error cargando conflictos:', err);
                }
            }

            function getFilteredConflictos() {
                const filterText = (txtSearchConflictos ? txtSearchConflictos.value : '').trim().toUpperCase();
                return allConflictos.filter(c => {
                    if (currentConflictTab === 'excel' && !c.enExcel) return false;
                    if (filterText && !c.ubicacion.toUpperCase().includes(filterText)) return false;
                    return true;
                });
            }

            function renderConflictSidebar() {
                if (!listConflictosUbis) return;
                listConflictosUbis.innerHTML = '';
                const items = getFilteredConflictos();

                if (items.length === 0) {
                    listConflictosUbis.innerHTML = `<div style="padding: 16px; text-align: center; color: var(--text-muted); font-size: 0.82rem;">No hay estantes en esta lista</div>`;
                    return;
                }

                items.forEach(c => {
                    const div = document.createElement('div');
                    div.className = `conflict-shelf-item ${selectedConflictUbi === c.ubicacion ? 'active' : ''}`;
                    div.innerHTML = `
                <span class="shelf-item-code">${escapeHtml(c.ubicacion)}</span>
                <div class="shelf-item-badges">
                    ${c.enExcel ? '<span class="badge-tag-excel">Excel</span>' : ''}
                    <span class="badge-conflict-cnt">${c.cantidad} prod</span>
                </div>
            `;
                    div.addEventListener('click', () => {
                        selectConflictUbi(c.ubicacion);
                    });
                    listConflictosUbis.appendChild(div);
                });
            }

            function selectConflictUbi(ubi) {
                selectedConflictUbi = ubi;
                renderConflictSidebar();

                const conf = conflictosByUbi.get(ubi.toUpperCase());
                if (!conf) {
                    if (containerConflictoProductos) {
                        containerConflictoProductos.innerHTML = `<div class="conflictos-empty-state">Estante resuelto o sin conflictos.</div>`;
                    }
                    if (lblConflictoShelfName) lblConflictoShelfName.textContent = ubi;
                    if (lblConflictoShelfBadge) lblConflictoShelfBadge.textContent = 'Sin conflictos activos';
                    return;
                }

                if (lblConflictoShelfName) lblConflictoShelfName.textContent = conf.ubicacion;
                if (lblConflictoShelfBadge) {
                    lblConflictoShelfBadge.textContent = `${conf.cantidad} productos compitiendo por este estante`;
                }

                // En pantallas móviles, desplazar automáticamente la vista hacia la cabecera del estante
                if (window.innerWidth <= 768 && pnlShelfHeader) {
                    pnlShelfHeader.scrollIntoView({ behavior: 'smooth' });
                }

                if (!containerConflictoProductos) return;
                containerConflictoProductos.innerHTML = '';

                conf.articulos.forEach(art => {
                    const card = document.createElement('div');
                    card.className = 'conflict-product-card';
                    const imgUrl = art.imagenUrl || `${API_BASE}/api/imagen?id=${encodeURIComponent(art.coArt)}`;

                    card.innerHTML = `
                <div class="cpc-left">
                    <img src="${imgUrl}" class="cpc-thumb" alt="" loading="lazy" onerror="this.src='${API_BASE}/api/imagen?id=placeholder';">
                    <div class="cpc-info">
                        <span class="cpc-title">${escapeHtml(art.descripcion)}</span>
                        <div class="cpc-meta">
                            <span>Código: <strong>${escapeHtml(art.coArt)}</strong></span>
                            ${art.codigoBarra ? `<span>Barras: <strong>${escapeHtml(art.codigoBarra)}</strong></span>` : ''}
                            <span>Stock Almacén: <strong class="cpc-stock-tag">${Number(art.existencia || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 })}</strong></span>
                        </div>
                        <div style="font-size: 0.76rem; color: var(--text-muted); margin-top: 2px;">
                            ${art.ubicacion2 ? `Historial previo: Estuvo en [${escapeHtml(art.ubicacion2)}]` : 'Sin historial previo'}
                        </div>
                    </div>
                </div>
                <div class="cpc-actions">
                    <button type="button" class="btn btn-mantener" title="Mantener este producto aquí y liberar a los demás de este estante">
                        ⭐ Mantener Aquí
                    </button>
                    <button type="button" class="btn btn-reasignar" title="Mover este producto a otro estante">
                        📦 Reasignar
                    </button>
                    <button type="button" class="btn btn-liberar" title="Quitar este producto del estante">
                        🗑️ Liberar
                    </button>
                </div>
            `;

                    // Mantener aquí
                    card.querySelector('.btn-mantener').addEventListener('click', async () => {
                        if (confirm(`¿Confirmar que [${art.coArt}] ${art.descripcion} se queda en el estante ${conf.ubicacion} y desalojar a los demás?`)) {
                            await ejecutarAccionConflicto(conf.ubicacion, art.coArt, 'mantener');
                        }
                    });

                    // Reasignar
                    card.querySelector('.btn-reasignar').addEventListener('click', async () => {
                        const nuevaUbi = prompt(`Ingrese la nueva ubicación para el artículo [${art.coArt}] ${art.descripcion}:`, '');
                        if (nuevaUbi && nuevaUbi.trim()) {
                            await ejecutarAccionConflicto(conf.ubicacion, art.coArt, 'reasignar', nuevaUbi.trim().toUpperCase());
                        }
                    });

                    // Liberar
                    card.querySelector('.btn-liberar').addEventListener('click', async () => {
                        if (confirm(`¿Liberar el producto [${art.coArt}] del estante ${conf.ubicacion}? (El estante quedará guardado en su historial)`)) {
                            await ejecutarAccionConflicto(conf.ubicacion, art.coArt, 'liberar');
                        }
                    });

                    containerConflictoProductos.appendChild(card);
                });
            }

            async function ejecutarAccionConflicto(ubi, coArt, accion, nuevaUbi = '') {
                try {
                    showToast('⏳ Aplicando resolución en Profit Plus...', 'info');
                    const res = await fetch(`${API_BASE}/api/conflictos/resolver`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ ubi, coArt, accion, nuevaUbi })
                    });
                    const data = await res.json();
                    if (res.ok && data.success) {
                        playBeep();
                        showToast(`✅ ${data.mensaje}`, 'success');
                        await loadConflictos(ubi);
                        if (currentProduct) {
                            searchProductSilent(currentProduct.coArt);
                        }
                    } else {
                        showToast(`❌ ${data.error || data.mensaje || 'Error al resolver conflicto'}`, 'error');
                    }
                } catch (err) {
                    showToast(`❌ Error de conexión: ${err.message}`, 'error');
                }
            }

            async function searchProductSilent(code) {
                try {
                    const response = await fetch(`${API_BASE}/api/articulo?q=${encodeURIComponent(code)}`);
                    if (response.ok) {
                        const results = await response.json();
                        if (results && results.length > 0) {
                            loadProductData(results[0]);
                        }
                    }
                } catch (_) { }
            }

            function openConflictosModal(ubiToSelect = null) {
                if (!modalConflictos) return;
                modalConflictos.classList.remove('hidden');
                loadConflictos(ubiToSelect);
            }

            function closeConflictosModal() {
                if (!modalConflictos) return;
                modalConflictos.classList.add('hidden');
            }

            // Eventos del módulo de conflictos
            if (btnOpenConflictosModal) {
                btnOpenConflictosModal.addEventListener('click', () => openConflictosModal());
            }
            if (btnCloseConflictosModal) {
                btnCloseConflictosModal.addEventListener('click', closeConflictosModal);
            }
            if (btnCloseConflictosFooter) {
                btnCloseConflictosFooter.addEventListener('click', closeConflictosModal);
            }
            if (btnResolverConflictoDirecto) {
                btnResolverConflictoDirecto.addEventListener('click', () => {
                    if (currentProduct && currentProduct.ubicacion1) {
                        openConflictosModal(currentProduct.ubicacion1);
                    } else {
                        openConflictosModal();
                    }
                });
            }
            if (tabConflictosExcel) {
                tabConflictosExcel.addEventListener('click', () => {
                    currentConflictTab = 'excel';
                    tabConflictosExcel.classList.add('active');
                    if (tabConflictosAll) tabConflictosAll.classList.remove('active');
                    renderConflictSidebar();
                });
            }
            if (tabConflictosAll) {
                tabConflictosAll.addEventListener('click', () => {
                    currentConflictTab = 'all';
                    tabConflictosAll.classList.add('active');
                    if (tabConflictosExcel) tabConflictosExcel.classList.remove('active');
                    renderConflictSidebar();
                });
            }
            if (txtSearchConflictos) {
                txtSearchConflictos.addEventListener('input', () => {
                    renderConflictSidebar();
                });
            }

            // ── MÓDULO CAMBIO DE FOTO DEL PRODUCTO ──
            function openModalPhoto() {
                if (!currentProduct) {
                    showToast('Primero busque y seleccione un producto para cambiar su foto', 'warning');
                    return;
                }

                if (lblModalPhotoSub) {
                    lblModalPhotoSub.textContent = `[${currentProduct.coArt}] ${currentProduct.descripcion}`;
                }

                if (txtPhotoUrl) txtPhotoUrl.value = '';
                if (filePhotoUpload) filePhotoUpload.value = '';
                uploadedPhotoBase64 = null;

                if (imgNewPhotoPreview) {
                    const currentImg = imgProductDetail ? imgProductDetail.src : `${API_BASE}/api/imagen?id=${encodeURIComponent(currentProduct.coArt)}`;
                    imgNewPhotoPreview.src = currentImg;
                }

                if (modalCambiarFoto) modalCambiarFoto.classList.remove('hidden');
                if (txtPhotoUrl) txtPhotoUrl.focus();
            }

            function closeModalPhoto() {
                if (modalCambiarFoto) modalCambiarFoto.classList.add('hidden');
                uploadedPhotoBase64 = null;
                const lblFileChosen = document.getElementById('lblFileChosen');
                if (lblFileChosen) lblFileChosen.textContent = 'Elegir de Galería o Tomar Foto';
                if (filePhotoUpload) filePhotoUpload.value = '';
            }

            if (btnCambiarFoto) {
                btnCambiarFoto.addEventListener('click', openModalPhoto);
            }
            if (btnCloseModalPhoto) {
                btnCloseModalPhoto.addEventListener('click', closeModalPhoto);
            }
            if (btnCancelModalPhoto) {
                btnCancelModalPhoto.addEventListener('click', closeModalPhoto);
            }

            if (btnPreviewPhotoUrl) {
                btnPreviewPhotoUrl.addEventListener('click', () => {
                    const url = (txtPhotoUrl ? txtPhotoUrl.value : '').trim();
                    if (!url) {
                        showToast('Ingrese una URL válida primero', 'warning');
                        return;
                    }
                    if (imgNewPhotoPreview) {
                        imgNewPhotoPreview.src = url;
                        imgNewPhotoPreview.onerror = () => {
                            showToast('⚠️ No se pudo cargar vista previa desde esa URL. Verifique el enlace.', 'warning');
                        };
                    }
                });
            }

            if (filePhotoUpload) {
                filePhotoUpload.addEventListener('change', (e) => {
                    const file = e.target.files && e.target.files[0];
                    if (!file) return;

                    const lblFileChosen = document.getElementById('lblFileChosen');
                    if (lblFileChosen) {
                        lblFileChosen.textContent = `✔ ${file.name}`;
                    }

                    const reader = new FileReader();
                    reader.onload = (evt) => {
                        uploadedPhotoBase64 = evt.target.result;
                        if (imgNewPhotoPreview) {
                            imgNewPhotoPreview.src = uploadedPhotoBase64;
                        }
                        if (txtPhotoUrl) txtPhotoUrl.value = '';
                    };
                    reader.readAsDataURL(file);
                });
            }

            if (btnSaveModalPhoto) {
                btnSaveModalPhoto.addEventListener('click', async () => {
                    if (!currentProduct) return;

                    const url = (txtPhotoUrl ? txtPhotoUrl.value : '').trim();
                    if (!url && !uploadedPhotoBase64) {
                        showToast('Debe ingresar una URL o seleccionar una imagen', 'warning');
                        return;
                    }

                    btnSaveModalPhoto.disabled = true;
                    showToast('⏳ Guardando foto del producto...', 'info');

                    try {
                        const payload = {
                            coArt: currentProduct.coArt,
                            codigoBarra: currentProduct.codigoBarra || '',
                            url: url || '',
                            base64: uploadedPhotoBase64 || ''
                        };

                        const res = await fetch(`${API_BASE}/api/imagen/actualizar`, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(payload)
                        });

                        const data = await res.json();
                        if (res.ok && data.success) {
                            playBeep();
                            showToast(`✅ ${data.mensaje}`, 'success');
                            closeModalPhoto();

                            // Actualizar imagen en la ficha principal con cache-buster
                            const newUrl = data.imageUrl ? `${API_BASE}${data.imageUrl}` : `${API_BASE}/api/imagen?id=${encodeURIComponent(currentProduct.coArt)}&v=${Date.now()}`;
                            currentProduct.imagenUrl = newUrl;
                            if (imgProductDetail) {
                                imgProductDetail.src = newUrl;
                                if (lblImageStatus) lblImageStatus.classList.remove('hidden');
                            }
                        } else {
                            showToast(`❌ ${data.error || 'Error al guardar la imagen'}`, 'error');
                        }
                    } catch (err) {
                        showToast(`❌ Error de conexión: ${err.message}`, 'error');
                    } finally {
                        btnSaveModalPhoto.disabled = false;
                    }
                });
            }

            // ── MODAL CONEXIÓN MÓVIL (QR) & BARRA INFERIOR MOBILE FIRST ──
            const btnOpenQrModal = document.getElementById('btnOpenQrModal');
            const modalQrConectar = document.getElementById('modalQrConectar');
            const btnCloseQrModal = document.getElementById('btnCloseQrModal');
            const btnCloseQrFooter = document.getElementById('btnCloseQrFooter');
            const imgQrCode = document.getElementById('imgQrCode');
            const lblQrUrlHttp = document.getElementById('lblQrUrlHttp');
            const lblQrUrlHttps = document.getElementById('lblQrUrlHttps');
            const btnCopyUrlHttp = document.getElementById('btnCopyUrlHttp');
            const btnCopyUrlHttps = document.getElementById('btnCopyUrlHttps');
            const btnConflictoBackMobile = document.getElementById('btnConflictoBackMobile');

            function openQrModal() {
                if (!modalQrConectar) return;

                const host = (location.hostname && location.hostname !== 'localhost' && location.hostname !== '127.0.0.1')
                    ? location.hostname
                    : serverLocalIp;

                const httpUrl = `http://${host}:${serverHttpPort}`;
                const httpsUrl = `https://${host}:${serverHttpsPort}`;

                if (lblQrUrlHttp) lblQrUrlHttp.textContent = httpUrl;
                if (lblQrUrlHttps) lblQrUrlHttps.textContent = httpsUrl;

                if (imgQrCode) {
                    imgQrCode.src = `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(httpUrl)}`;
                }

                modalQrConectar.classList.remove('hidden');
            }

            function closeQrModal() {
                if (modalQrConectar) modalQrConectar.classList.add('hidden');
            }

            if (btnOpenQrModal) btnOpenQrModal.addEventListener('click', openQrModal);
            if (btnCloseQrModal) btnCloseQrModal.addEventListener('click', closeQrModal);
            if (btnCloseQrFooter) btnCloseQrFooter.addEventListener('click', closeQrModal);

            if (btnCopyUrlHttp) {
                btnCopyUrlHttp.addEventListener('click', () => {
                    const url = lblQrUrlHttp ? lblQrUrlHttp.textContent : '';
                    navigator.clipboard.writeText(url).then(() => {
                        showToast('📋 Enlace HTTP copiado al portapapeles', 'success');
                    }).catch(() => {
                        showToast(`Enlace: ${url}`, 'info');
                    });
                });
            }

            if (btnCopyUrlHttps) {
                btnCopyUrlHttps.addEventListener('click', () => {
                    const url = lblQrUrlHttps ? lblQrUrlHttps.textContent : '';
                    navigator.clipboard.writeText(url).then(() => {
                        showToast('📋 Enlace HTTPS copiado al portapapeles', 'success');
                    }).catch(() => {
                        showToast(`Enlace: ${url}`, 'info');
                    });
                });
            }

            if (btnConflictoBackMobile) {
                btnConflictoBackMobile.addEventListener('click', () => {
                    if (listConflictosUbis) {
                        listConflictosUbis.scrollIntoView({ behavior: 'smooth' });
                        if (txtSearchConflictos) txtSearchConflictos.focus();
                    }
                });
            }

            // Navegación Inferior Móvil (Mobile First Bottom Bar)
            const navBtnBuscar = document.getElementById('navBtnBuscar');
            const navBtnAsignar = document.getElementById('navBtnAsignar');
            const navBtnConflictos = document.getElementById('navBtnConflictos');
            const navBtnQr = document.getElementById('navBtnQr');

            if (navBtnBuscar) {
                navBtnBuscar.addEventListener('click', () => {
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                    if (txtSearch) {
                        txtSearch.focus();
                        txtSearch.select();
                    }
                });
            }

            if (navBtnAsignar) {
                navBtnAsignar.addEventListener('click', () => {
                    if (currentProduct) {
                        openAsignarModalWithProduct(currentProduct);
                    } else {
                        openAsignarModal();
                    }
                });
            }

            if (navBtnConflictos) {
                navBtnConflictos.addEventListener('click', () => {
                    openConflictosModal();
                });
            }

            if (navBtnQr) {
                navBtnQr.addEventListener('click', () => {
                    openQrModal();
                });
            }

            // Carga inicial en segundo plano de conflictos para mostrar badge en navbar
            loadConflictos();
        });
