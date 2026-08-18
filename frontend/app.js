const API_BASE_URL = 'http://localhost:8080/api/v1';
let currentSession = { email: '', role: '', institutionId: '88888888-4444-4444-4444-121212121212' };

let activeTutors = [];
let activeStudents = [];
let activeStaff = [];

let currentTutorData = null;
let currentStaffData = null;
let currentStudentData = null;

let navigationHistory = [];

function pushNavigation(viewType, entityId = null) {
  const last = navigationHistory[navigationHistory.length - 1];
  if (!last || last.viewType !== viewType || last.entityId !== entityId) {
    navigationHistory.push({ viewType, entityId });
  }
}

// Variables Globales del Módulo de Cuotas
let selectedStudentForCuotas = null;
let studentFeesList = [];
let institutionalEmails = {
  receiptEmail: 'administracion@onceunidos.com',
  feeQueryEmail: 'tesoreria@onceunidos.com'
};

// Variables Globales del Módulo de Retiros y Restricciones
let selectedStudentForRetiros = null;
let currentStudentPickups = [];
let selectedStudentForRestricciones = null;
let currentStudentRestrictions = [];

// ========================================================
// AUTENTICACIÓN Y NAVEGACIÓN
// ========================================================

function handleLogin() {
  const email = document.getElementById('emailInput').value.trim();
  if (!email) return;

  let role = 'TEACHER';
  if (email.includes('direccion') || email.includes('director')) role = 'DIRECTOR';
  else if (email.includes('admin')) role = 'ADMINISTRATIVE';
  else if (email.includes('preceptor')) role = 'PRECEPTOR';
  else if (email.includes('tutor') || email.includes('padre')) role = 'TUTOR';

  currentSession.email = email;
  currentSession.role = role;

  document.getElementById('userDisplay').innerText = email;
  document.getElementById('roleBadge').innerText =
      role === 'DIRECTOR' ? 'Directora' :
          (role === 'ADMINISTRATIVE' ? 'Administrativo' :
              (role === 'PRECEPTOR' ? 'Preceptor/a' :
                  (role === 'TUTOR' ? 'Tutor' : 'Docente')));

  document.getElementById('loginPage').classList.add('hidden');
  document.getElementById('mainDashboard').classList.remove('hidden');

  refreshAllData();
}

function handleLogout() { location.reload(); }

function showSection(sectionId, clearHistory = true) {
  if (clearHistory) {
    navigationHistory = [];
  }

  document.querySelectorAll('.dashboard-view').forEach(v => v.classList.add('hidden'));

  const targetView = document.getElementById(sectionId);
  if (targetView) targetView.classList.remove('hidden');

  document.querySelectorAll('aside nav button').forEach(b => b.classList.remove('bg-emerald-50', 'text-emerald-700'));
  const btn = document.getElementById(`nav-${sectionId}`);
  if (btn) btn.classList.add('bg-emerald-50', 'text-emerald-700');

  if (sectionId === 'inicioView') renderInicioFeed();
  if (sectionId === 'cuotasView') renderCuotasView();
  if (sectionId === 'retirosView') renderRetirosView();
  if (sectionId === 'restriccionesView') renderRestriccionesView();
  if (sectionId === 'comunicadosView') renderComunicadosView();
}

function toggleForm(id) { document.getElementById(id).classList.toggle('hidden'); }

function refreshAllData() {
  fetchTutors();
  fetchStudents();
  fetchStaff();
  fetchAnnouncements();
}

function getInitials(name) {
  if (!name) return '--';
  return name.trim().split(/\s+/).slice(0, 2).map(n => n[0].toUpperCase()).join('');
}

function normalizarTexto(txt) {
  if (!txt) return '';
  return txt
      .toString()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
}

// ========================================================
// GESTIÓN DE TUTORES
// ========================================================

async function fetchTutors() {
  try {
    const r = await fetch(`${API_BASE_URL}/tutors`, {
      headers: { 'X-Institution-Id': currentSession.institutionId, 'X-User-Role': currentSession.role }
    });
    activeTutors = r.ok ? await r.json() : [];
    document.getElementById('countTutorsBadge').innerText = activeTutors.length;

    document.getElementById('tutorsTableBody').innerHTML = activeTutors.map(t => `
      <tr class="hover:bg-slate-50/80 transition-colors">
        <td class="p-3 font-semibold text-slate-900">${t.lastName || ''}, ${t.firstName || ''}</td>
        <td class="p-3 text-slate-600 font-medium">${t.documentNumber || '--'}</td>
        <td class="p-3"><span class="bg-amber-100 text-amber-800 text-xs font-bold px-2.5 py-1 rounded-md">${t.relationship || 'Tutor'}</span></td>
        <td class="p-3 text-center">
          <button onclick="viewTutorProfile('${t.id}')" class="text-slate-400 hover:text-emerald-600 p-1 rounded-full hover:bg-slate-100 cursor-pointer">
            <span class="material-icons-outlined">contact_page</span>
          </button>
        </td>
      </tr>
    `).join('');

    populateTutorSelects();
  } catch(e) { console.error("Error consultando tutores:", e); }
}

function populateTutorSelects() {
  const options = '<option value="">-- Seleccionar Tutor --</option>' + activeTutors.map(t => `<option value="${t.id}">${t.lastName}, ${t.firstName} (${t.relationship || 'Tutor'})</option>`).join('');
  const t1 = document.getElementById('studentTutor1');
  const t2 = document.getElementById('studentTutor2');
  if(t1) t1.innerHTML = options;
  if(t2) t2.innerHTML = options;
}

async function submitTutor() {
  const payload = {
    firstName: document.getElementById('tutorFirstName').value.trim(),
    lastName: document.getElementById('tutorLastName').value.trim(),
    documentNumber: document.getElementById('tutorDni').value.trim(),
    relationship: document.getElementById('tutorRelationship').value,
    phone: document.getElementById('tutorPhone').value.trim(),
    email: document.getElementById('tutorEmail').value.trim()
  };
  await fetch(`${API_BASE_URL}/tutors`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Institution-Id': currentSession.institutionId, 'X-User-Role': currentSession.role },
    body: JSON.stringify(payload)
  });
  toggleForm('tutorFormContainer');
  fetchTutors();
}

async function viewTutorProfile(id, isBackNavigation = false) {
  const t = activeTutors.find(item => item.id === id);
  if(!t) {
    alert("No se encontraron los datos de este tutor.");
    return;
  }

  if (!isBackNavigation) {
    if (currentStudentData && !document.getElementById('studentProfileView').classList.contains('hidden')) {
      pushNavigation('studentProfileView', currentStudentData.id);
    } else {
      pushNavigation('tutorsView', null);
    }
  }

  let hijosVinculados = activeStudents.filter(s => {
    if (Array.isArray(s.tutorIds) && s.tutorIds.includes(t.id)) return true;
    if (Array.isArray(s.tutors) && s.tutors.some(tut => (tut.id === t.id || tut.tutorId === t.id))) return true;
    if (Array.isArray(s.tutores) && s.tutores.some(tut => (tut.id === t.id || tut.tutorId === t.id))) return true;
    if (s.tutorId === t.id || s.primaryTutorId === t.id || s.secondaryTutorId === t.id) return true;
    return false;
  }).map(s => ({
    id: s.id,
    nombre: s.firstName,
    apellido: s.lastName,
    curso: s.classroom,
    parentesco: t.relationship || 'Hijo/a'
  }));

  if (hijosVinculados.length === 0) {
    try {
      const res = await fetch(`${API_BASE_URL}/tutors/${t.id}/students`, {
        headers: { 'X-Institution-Id': currentSession.institutionId, 'X-User-Role': currentSession.role }
      });
      if (res.ok) {
        const dataHijos = await res.json();
        if (Array.isArray(dataHijos) && dataHijos.length > 0) {
          hijosVinculados = dataHijos.map(s => ({
            id: s.id,
            nombre: s.firstName,
            apellido: s.lastName,
            curso: s.classroom,
            parentesco: t.relationship || 'Hijo/a'
          }));
        }
      }
    } catch (err) {
      console.warn("No se pudo consultar el endpoint de hijos:", err);
    }
  }

  setTutorData({
    id: t.id,
    nombre: t.firstName,
    apellido: t.lastName,
    vinculo: t.relationship || 'Tutor Legal',
    dni: t.documentNumber,
    nacionalidad: t.nacionalidad || 'Argentina',
    profesion: t.profesion || '-',
    condicionActividad: t.condicionActividad || 'Trabaja',
    celular: t.phone || '-',
    telefonoFijo: t.phoneFijo || '-',
    email: t.email || '-',
    conviveEstudiante: t.convive || 'Sí',
    domicilio: t.direccion || '-',
    hijos: hijosVinculados
  });
}

function setTutorData(data) {
  if (!data) return;
  currentTutorData = data;

  document.querySelectorAll('.dashboard-view').forEach(v => v.classList.add('hidden'));
  document.getElementById('tutorProfileView').classList.remove('hidden');

  const nombreCompleto = `${data.nombre || data.firstName || ''} ${data.apellido || data.lastName || ''}`.trim() || 'Sin Nombre';
  document.getElementById('tutor-nombre').textContent = nombreCompleto;
  document.getElementById('tutor-avatar').textContent = getInitials(nombreCompleto);
  document.getElementById('tutor-vinculo').textContent = `Vínculo: ${data.vinculo || data.relationship || '-'}`;
  document.getElementById('tutor-dni').textContent = `DNI: ${data.dni || data.documentNumber || '-'}`;

  document.getElementById('tutor-nacionalidad').textContent = data.nacionalidad || '-';
  document.getElementById('tutor-profesion').textContent = data.profesion || '-';
  document.getElementById('tutor-actividad').textContent = data.condicionActividad || '-';
  document.getElementById('tutor-celular').textContent = data.celular || data.phone || '-';
  document.getElementById('tutor-fijo').textContent = data.telefonoFijo || '-';
  document.getElementById('tutor-email').textContent = data.email || '-';
  document.getElementById('tutor-convive').textContent = data.conviveEstudiante || data.convive || 'Sí';
  document.getElementById('tutor-domicilio').textContent = data.domicilio || data.direccion || '-';

  const hijosCont = document.getElementById('hijos-container');
  hijosCont.innerHTML = '';

  if (data.hijos && data.hijos.length > 0) {
    data.hijos.forEach(h => {
      const item = document.createElement('div');
      item.className = 'student-item bg-slate-50 border border-slate-200 rounded-lg p-3 flex justify-between items-center cursor-pointer hover:border-emerald-500 transition-all';
      item.onclick = () => showStudentProfile(h.id);
      item.innerHTML = `
        <div>
            <h4 class="font-bold text-slate-800 text-sm">${h.nombre || h.firstName} ${h.apellido || h.lastName}</h4>
            <p class="text-xs text-slate-500">Curso: ${h.curso || h.classroom || '-'}</p>
        </div>
        <span class="bg-emerald-100 text-emerald-800 text-[10px] font-bold px-2 py-0.5 rounded">${h.parentesco || 'Hijo/a'}</span>`;
      hijosCont.appendChild(item);
    });
  } else {
    hijosCont.innerHTML = '<p class="text-xs text-slate-400 col-span-2">No hay alumnos vinculados registrados.</p>';
  }

  evaluarPermisosPerfilTutor();
}

function hideTutorProfile() {
  document.getElementById('tutorProfileView').classList.add('hidden');

  const previous = navigationHistory.pop();
  if (previous) {
    if (previous.viewType === 'studentProfileView' && previous.entityId) {
      showStudentProfile(previous.entityId, true);
    } else {
      showSection(previous.viewType, false);
    }
  } else {
    showSection('tutorsView', true);
  }
}

function evaluarPermisosPerfilTutor() {
  const btnEdit = document.getElementById('btn-edit-tutor');
  const btnBaja = document.getElementById('btn-baja-tutor');
  const role = currentSession.role;

  if (btnEdit) {
    if (['DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER', 'TUTOR'].includes(role)) {
      btnEdit.classList.remove('hidden');
    } else {
      btnEdit.classList.add('hidden');
    }
  }

  if (btnBaja) {
    if (['DIRECTOR', 'ADMINISTRATIVE'].includes(role)) {
      btnBaja.classList.remove('hidden');
    } else {
      btnBaja.classList.add('hidden');
    }
  }
}

function abrirModalEdicionTutor() {
  if (!currentTutorData) return;
  document.getElementById('edit-nombre').value = currentTutorData.nombre || currentTutorData.firstName || '';
  document.getElementById('edit-apellido').value = currentTutorData.apellido || currentTutorData.lastName || '';
  document.getElementById('edit-vinculo').value = currentTutorData.vinculo || currentTutorData.relationship || 'Madre';
  document.getElementById('edit-dni').value = currentTutorData.dni || currentTutorData.documentNumber || '';
  document.getElementById('edit-nacionalidad').value = currentTutorData.nacionalidad || 'Argentina';
  document.getElementById('edit-profesion').value = currentTutorData.profesion || '';
  document.getElementById('edit-actividad').value = currentTutorData.condicionActividad || 'Trabaja';
  document.getElementById('edit-celular').value = currentTutorData.celular || currentTutorData.phone || '';
  document.getElementById('edit-email').value = currentTutorData.email || '';
  document.getElementById('edit-calle').value = currentTutorData.domicilio || currentTutorData.direccion || '';
  document.getElementById('edit-convive').value = currentTutorData.conviveEstudiante || currentTutorData.convive || 'Sí';

  document.getElementById('tutorEditModal').classList.remove('hidden');
}

function cerrarModalEdicionTutor() {
  document.getElementById('tutorEditModal').classList.add('hidden');
}

async function guardarDatosTutor(e) {
  e.preventDefault();
  if (!currentTutorData || !currentTutorData.id) return;

  const updatedData = {
    ...currentTutorData,
    firstName: document.getElementById('edit-nombre').value.trim(),
    lastName: document.getElementById('edit-apellido').value.trim(),
    relationship: document.getElementById('edit-vinculo').value,
    documentNumber: document.getElementById('edit-dni').value.trim(),
    nacionalidad: document.getElementById('edit-nacionalidad').value.trim(),
    profesion: document.getElementById('edit-profesion').value.trim(),
    condicionActividad: document.getElementById('edit-actividad').value,
    phone: document.getElementById('edit-celular').value.trim(),
    email: document.getElementById('edit-email').value.trim(),
    direccion: document.getElementById('edit-calle').value.trim(),
    convive: document.getElementById('edit-convive').value
  };

  try {
    const response = await fetch(`${API_BASE_URL}/tutors/${currentTutorData.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      },
      body: JSON.stringify(updatedData)
    });

    let finalTutor = updatedData;
    if (response.ok) {
      try {
        const json = await response.json();
        if (json && json.id) finalTutor = { ...updatedData, ...json };
      } catch (_) {}
    }

    const idx = activeTutors.findIndex(t => t.id === currentTutorData.id);
    if (idx !== -1) {
      activeTutors[idx] = { ...activeTutors[idx], ...finalTutor };
    }

    cerrarModalEdicionTutor();
    setTutorData(finalTutor);
    await fetchTutors();

  } catch (error) {
    console.error("Error al actualizar tutor:", error);
    cerrarModalEdicionTutor();
    setTutorData(updatedData);
  }
}

async function confirmarBajaTutor() {
  if (!currentTutorData || !currentTutorData.id) {
    alert("No se pudo identificar la ficha del tutor a dar de baja.");
    return;
  }

  const nombreCompleto = `${currentTutorData.nombre || currentTutorData.firstName || ''} ${currentTutorData.apellido || currentTutorData.lastName || ''}`.trim();

  const confirmacion = confirm(
      `¿Estás seguro de que deseas dar de baja al tutor "${nombreCompleto}"?\n\n` +
      `ATENCIÓN: Si este tutor es el ÚNICO responsable asignado a sus alumnos vinculados, los alumnos también serán dados de baja e ingresados al registro histórico automáticamente.`
  );

  if (!confirmacion) return;

  try {
    const response = await fetch(`${API_BASE_URL}/tutors/${currentTutorData.id}/baja`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      }
    });

    if (response.ok || response.status === 200 || response.status === 204) {
      alert(`Baja procesada con éxito.\nEl tutor y los alumnos sin otros tutores asignados pasaron al registro histórico.`);
      hideTutorProfile();
      await refreshAllData();
      showSection('tutorsView');
    } else {
      const errorMsg = await response.text();
      alert(`No se pudo procesar la baja en el servidor: ${errorMsg || 'Error desconocido'}`);
    }

  } catch (error) {
    console.error("Error al procesar la baja del tutor:", error);
    alert("Hubo un fallo de red o conexión con el servidor al intentar dar de baja.");
  }
}

// ========================================================
// GESTIÓN DE ALUMNOS
// ========================================================

async function fetchStudents() {
  try {
    const r = await fetch(`${API_BASE_URL}/students`, {
      headers: { 'X-Institution-Id': currentSession.institutionId, 'X-User-Role': currentSession.role }
    });
    activeStudents = r.ok ? await r.json() : [];
    renderStudentsTable(activeStudents);
  } catch(e) {
    console.error("Error al consultar estudiantes:", e);
    renderStudentsTable([]);
  }
}

function renderStudentsTable(list) {
  document.getElementById('countStudentsBadge').innerText = list.length;
  const tbody = document.getElementById('studentsTableBody');

  if (!list || list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" class="p-4 text-center text-slate-400 text-xs">No hay alumnos registrados.</td></tr>`;
    return;
  }

  tbody.innerHTML = list.map(s => `
    <tr class="hover:bg-slate-50/80 transition-colors">
      <td class="p-3 font-semibold text-slate-900">${s.lastName || ''}, ${s.firstName || ''}</td>
      <td class="p-3 text-slate-600 font-medium">${s.documentNumber || '--'}</td>
      <td class="p-3"><span class="bg-blue-50 text-blue-700 text-xs font-bold px-2.5 py-1 rounded-full">${s.classroom || 'Sin asignar'}</span></td>
      <td class="p-3 text-center">
        <button onclick="showStudentProfile('${s.id}')" class="text-slate-400 hover:text-emerald-600 p-1 rounded-full hover:bg-slate-100 cursor-pointer">
          <span class="material-icons-outlined">contact_page</span>
        </button>
      </td>
    </tr>
  `).join('');
}

function filterStudentsTable() {
  const target = document.getElementById('filterStudentClassroom').value;
  if(target === 'TODAS') { renderStudentsTable(activeStudents); }
  else { renderStudentsTable(activeStudents.filter(s => s.classroom === target)); }
}

function openEditStudentModal() {
  if (!currentStudentData) {
    alert("No se pudo cargar la información del alumno para editar.");
    return;
  }

  document.getElementById('edit-student-nombre').value = currentStudentData.firstName || '';
  document.getElementById('edit-student-apellido').value = currentStudentData.lastName || '';
  document.getElementById('edit-student-legajo').value = currentStudentData.legajoNumber || currentStudentData.legajo || '';
  document.getElementById('edit-student-dni').value = currentStudentData.documentNumber || '';
  document.getElementById('edit-student-nacimiento').value = currentStudentData.birthDate || '';
  document.getElementById('edit-student-classroom').value = currentStudentData.classroom || 'Maternal';
  document.getElementById('edit-student-direccion').value = currentStudentData.address || currentStudentData.direccion || '';

  document.getElementById('studentEditModal').classList.remove('hidden');
}

function closeEditStudentModal() {
  document.getElementById('studentEditModal').classList.add('hidden');
}

async function guardarDatosAlumno(e) {
  e.preventDefault();
  if (!currentStudentData || !currentStudentData.id) return;

  const legajoVal = document.getElementById('edit-student-legajo').value.trim();
  const dirVal = document.getElementById('edit-student-direccion').value.trim();

  const updatedPayload = {
    ...currentStudentData,
    firstName: document.getElementById('edit-student-nombre').value.trim(),
    lastName: document.getElementById('edit-student-apellido').value.trim(),
    legajoNumber: legajoVal,
    legajo: legajoVal,
    documentNumber: document.getElementById('edit-student-dni').value.trim(),
    birthDate: document.getElementById('edit-student-nacimiento').value,
    classroom: document.getElementById('edit-student-classroom').value,
    address: dirVal,
    direccion: dirVal
  };

  try {
    const response = await fetch(`${API_BASE_URL}/students/${currentStudentData.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      },
      body: JSON.stringify(updatedPayload)
    });

    if (!response.ok) throw new Error("Error al actualizar la ficha del alumno en el servidor");

    alert("¡Ficha del alumno actualizada con éxito!");
    closeEditStudentModal();

    await fetchStudents();
    await showStudentProfile(currentStudentData.id, true);

  } catch (error) {
    console.error("Error al actualizar alumno:", error);
    alert("No se pudieron guardar los cambios. Revisa la conexión con el servidor.");
  }
}

async function showStudentProfile(studentId, isBackNavigation = false) {
  try {
    const response = await fetch(`${API_BASE_URL}/students/${studentId}`, {
      headers: { 'X-Institution-Id': currentSession.institutionId, 'X-User-Role': currentSession.role }
    });

    if (!response.ok) throw new Error("Error al consultar el perfil");
    const data = await response.json();

    if (!isBackNavigation) {
      if (currentTutorData && !document.getElementById('tutorProfileView').classList.contains('hidden')) {
        pushNavigation('tutorProfileView', currentTutorData.id);
      } else {
        pushNavigation('alumnosView', null);
      }
    }

    currentStudentData = data;

    document.querySelectorAll('.dashboard-view').forEach(view => view.classList.add('hidden'));
    document.getElementById('studentProfileView').classList.remove('hidden');

    const nombre = data.firstName || '';
    const apellido = data.lastName || '';
    document.getElementById('alumno-nombre').textContent = `${nombre} ${apellido}`.trim() || '-';
    document.getElementById('alumno-avatar').textContent = getInitials(`${nombre} ${apellido}`);

    const legajoVisual = data.legajoNumber || data.legajo || (data.id ? data.id.substring(0, 8) : '-');
    document.getElementById('alumno-legajo').textContent = `Legajo: ${legajoVisual}`;
    document.getElementById('alumno-curso').textContent = `Salita: ${data.classroom || '-'}`;
    document.getElementById('alumno-estado').textContent = `Estado: ${data.status || 'ACTIVO'}`;
    document.getElementById('alumno-dni').textContent = data.documentNumber || '-';
    document.getElementById('alumno-nacimiento').textContent = data.birthDate || '-';
    document.getElementById('alumno-domicilio').textContent = data.address || data.direccion || '-';

    const btnBajaAlumno = document.getElementById('btn-baja-alumno');
    if (btnBajaAlumno) {
      if (['DIRECTOR', 'ADMINISTRATIVE'].includes(currentSession.role)) {
        btnBajaAlumno.classList.remove('hidden');
      } else {
        btnBajaAlumno.classList.add('hidden');
      }
    }

    const tutoresCont = document.getElementById('tutores-container');
    tutoresCont.innerHTML = '';
    let listaTutores = data.tutors || data.tutores || [];

    if (listaTutores.length === 0 && activeTutors.length > 0 && data.tutorIds) {
      listaTutores = activeTutors.filter(t => data.tutorIds.includes(t.id));
    }

    if (listaTutores.length > 0) {
      listaTutores.forEach((t, index) => {
        const isPrimary = index === 0;
        const card = document.createElement('div');
        card.className = "p-4 bg-white border border-slate-200 rounded-xl shadow-xs hover:border-emerald-500 transition-all cursor-pointer flex justify-between items-center group mb-2";

        const tutorIdTarget = t.id || t.tutorId;
        card.onclick = () => viewTutorProfile(tutorIdTarget);

        card.innerHTML = `
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold text-sm">
              ${getInitials(`${t.firstName || ''} ${t.lastName || ''}`)}
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h4 class="font-bold text-slate-900 group-hover:text-emerald-700">${t.lastName || ''}, ${t.firstName || 'Tutor'}</h4>
                <span class="bg-emerald-50 text-emerald-700 border border-emerald-200 text-[10px] font-bold px-2 py-0.5 rounded-md">${isPrimary ? 'Contacto Emergencia N°1' : 'Contacto Emergencia N°2'}</span>
              </div>
              <p class="text-xs text-slate-500 mt-1">
                Vínculo: ${t.relationship || 'Tutor Legal'} | DNI: ${t.documentNumber || '--'} | Tel: ${t.phone || 'Sin Registrar'}
              </p>
            </div>
          </div>
          <span class="material-icons-outlined text-slate-400 group-hover:text-emerald-600">chevron_right</span>
        `;
        tutoresCont.appendChild(card);
      });
    } else {
      tutoresCont.innerHTML = `<p class="text-xs text-slate-400 italic p-3">No hay tutores vinculados a este alumno.</p>`;
    }

    await fetchAndRenderPickupsGeneral(studentId, 'autorizados-container');
    await fetchAndRenderRestrictionsGeneral(studentId, 'restricciones-container');
    await renderComunicadosPrivadosAlumno(studentId);

  } catch (error) {
    console.error("Error al cargar perfil de alumno:", error);
    alert("No se pudo cargar la ficha del alumno.");
  }
}

function hideStudentProfile() {
  document.getElementById('studentProfileView').classList.add('hidden');

  const previous = navigationHistory.pop();
  if (previous) {
    if (previous.viewType === 'tutorProfileView' && previous.entityId) {
      viewTutorProfile(previous.entityId, true);
    } else {
      showSection(previous.viewType, false);
    }
  } else {
    showSection('alumnosView', true);
  }
}

async function confirmarBajaAlumno() {
  const studentId = currentStudentData?.id;

  const student = activeStudents.find(s => s.id === studentId);
  if (!student) return;

  const confirmacion = confirm(`¿Estás seguro de dar de baja al alumno "${student.firstName} ${student.lastName}"?\n\nEl legajo pasará al registro histórico.`);
  if (!confirmacion) return;

  try {
    const response = await fetch(`${API_BASE_URL}/students/${studentId}/baja`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      }
    });

    if (!response.ok) throw new Error("Error en la solicitud de baja");

    alert("El alumno ha sido dado de baja correctamente.");
    hideStudentProfile();
    refreshAllData();

  } catch (error) {
    console.error("Error al dar de baja alumno:", error);
    alert("No se pudo dar de baja al alumno en el servidor.");
  }
}

async function submitStudent() {
  const birthDateValue = document.getElementById('studentBirthDate')?.value;

  if (!birthDateValue) {
    alert("La fecha de nacimiento es obligatoria para registrar la matrícula.");
    return;
  }

  const selectedTutors = getSelectedTutors();
  if (selectedTutors.length === 0) {
    alert("Debes seleccionar al menos 1 tutor responsable.");
    return;
  }

  const legajoVal = document.getElementById('studentLegajo')?.value.trim() || '';
  const dirVal = document.getElementById('studentDireccion')?.value.trim() || '';

  const studentData = {
    firstName: document.getElementById('studentFirstName')?.value.trim() || '',
    lastName: document.getElementById('studentLastName')?.value.trim() || '',
    legajoNumber: legajoVal,
    legajo: legajoVal,
    documentNumber: document.getElementById('studentDni')?.value.trim() || '',
    birthDate: birthDateValue,
    classroom: document.getElementById('studentClassroom')?.value || '',
    address: dirVal,
    direccion: dirVal,
    status: "ACTIVE"
  };

  try {
    const response = await fetch(`${API_BASE_URL}/students`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      },
      body: JSON.stringify(studentData)
    });

    if (!response.ok) throw new Error("Error al guardar el alumno en el servidor");
    const alumnoCreado = await response.json();

    if (alumnoCreado.id) {
      await fetch(`${API_BASE_URL}/students/${alumnoCreado.id}/tutors`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Institution-Id': currentSession.institutionId,
          'X-User-Role': currentSession.role
        },
        body: JSON.stringify(selectedTutors)
      });
    }

    alert("¡Matrícula y Tutores vinculados correctamente!");
    toggleForm('studentFormContainer');
    refreshAllData();

  } catch (error) {
    console.error("Error en la matrícula:", error);
    alert("No se pudo registrar la matrícula. Revisa los datos ingresados.");
  }
}

function getSelectedTutors() {
  const tutor1Id = document.getElementById('studentTutor1')?.value;
  const tutor2Id = document.getElementById('studentTutor2')?.value;

  const tutors = [];
  if (tutor1Id) tutors.push({ tutorId: tutor1Id, isPrimary: true });
  if (tutor2Id && tutor2Id !== tutor1Id) tutors.push({ tutorId: tutor2Id, isPrimary: false });

  if (tutors.length > 2) {
    alert("Un alumno solo puede tener como máximo 2 tutores legales asignados.");
    return tutors.slice(0, 2);
  }
  return tutors;
}

// ========================================================
// GESTIÓN DE PERSONAL / STAFF
// ========================================================

function renderStaffTable(list) {
  const countBadge = document.getElementById('countStaffBadge');
  if (countBadge) countBadge.innerText = list ? list.length : 0;

  const tbody = document.getElementById('staffTableBody');
  if (!tbody) return;

  if (!list || list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" class="p-4 text-center text-slate-400 text-xs">No hay miembros del personal registrados.</td></tr>`;
    return;
  }

  tbody.innerHTML = list.map(u => `
    <tr class="hover:bg-slate-50/80 transition-colors">
      <td class="p-3 font-semibold text-slate-900">${u.lastName || ''}, ${u.firstName || ''}</td>
      <td class="p-3 text-slate-600 font-medium">${u.email || '--'}</td>
      <td class="p-3">
        <span class="bg-emerald-50 text-emerald-700 text-xs font-bold px-2 py-0.5 rounded">${u.role || 'STAFF'}</span>
        ${u.classroom ? `<span class="bg-blue-50 text-blue-700 text-[10px] font-bold px-1.5 py-0.5 rounded ml-1">${u.classroom}</span>` : ''}
      </td>
      <td class="p-3 text-center">
        <button onclick="viewStaffProfile('${u.id}')" class="text-slate-400 hover:text-emerald-600 p-1 rounded-full hover:bg-slate-100 cursor-pointer">
          <span class="material-icons-outlined">contact_page</span>
        </button>
      </td>
    </tr>
  `).join('');
}

async function fetchStaff() {
  try {
    const r = await fetch(`${API_BASE_URL}/institution/staff`, {
      headers: {
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      }
    });
    activeStaff = r.ok ? await r.json() : [];
    renderStaffTable(activeStaff);

  } catch(e) {
    console.error("Error al obtener el personal:", e);
    renderStaffTable([]);
  }
}

function handleStaffRoleFormChange(role, targetBlockId) {
  const block = document.getElementById(targetBlockId);
  if (!block) return;

  if (role === 'TEACHER') {
    block.classList.remove('hidden');
  } else {
    block.classList.add('hidden');
  }
}

function filterStaffTable() {
  const targetRole = document.getElementById('filterStaffRole')?.value;
  if (!targetRole || targetRole === 'TODOS') {
    renderStaffTable(activeStaff);
  } else {
    renderStaffTable(activeStaff.filter(u => u.role === targetRole));
  }
}

async function submitStaff() {
  const role = document.getElementById('staffRole')?.value;
  const hireDateValue = document.getElementById('staffHireDate')?.value;

  const firstName = document.getElementById('staffFirstName')?.value.trim() || '';
  const lastName = document.getElementById('staffLastName')?.value.trim() || '';
  const email = document.getElementById('staffEmail')?.value.trim() || '';

  if (!firstName || !lastName) {
    alert("El nombre y apellido son obligatorios.");
    return;
  }

  if (!hireDateValue) {
    alert("La fecha de contratación es obligatoria.");
    return;
  }

  const payload = {
    id: crypto.randomUUID(),
    firstName: firstName,
    lastName: lastName,
    email: email,
    password: "123",
    role: role,
    hireDate: hireDateValue,
    classroom: role === 'TEACHER' ? document.getElementById('staffAssignedClassroom')?.value : null,
    tenantId: currentSession.institutionId
  };

  try {
    const response = await fetch(`${API_BASE_URL}/institution/staff`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      },
      body: JSON.stringify(payload)
    });

    if (!response.ok) throw new Error("Error al registrar personal en el servidor");

    let savedObject = payload;
    try {
      const responseData = await response.json();
      if (responseData && (responseData.id || responseData.email)) {
        savedObject = responseData;
      }
    } catch (jsonErr) {}

    const index = activeStaff.findIndex(item => item.id === savedObject.id || item.email === savedObject.email);
    if (index >= 0) {
      activeStaff[index] = savedObject;
    } else {
      activeStaff.push(savedObject);
    }

    renderStaffTable(activeStaff);

    document.getElementById('staffFirstName').value = '';
    document.getElementById('staffLastName').value = '';
    document.getElementById('staffEmail').value = '';
    document.getElementById('staffHireDate').value = '';
    toggleForm('staffFormContainer');

    alert("¡Miembro del personal registrado correctamente!");

    setTimeout(() => {
      fetchStaff();
    }, 500);

  } catch (error) {
    console.error("Error en alta de personal:", error);
    alert("Hubo un problema al registrar al miembro del personal. Revisa los datos o la conexión.");
  }
}

function viewStaffProfile(id) {
  const staffMember = activeStaff.find(u => u.id === id);
  if (!staffMember) {
    alert("No se encontraron los datos del legajo seleccionado.");
    return;
  }
  currentStaffData = staffMember;

  const cursosAsignados = staffMember.classroom ? [{
    nombre: staffMember.classroom,
    turno: "Turno Mañana",
    cantidadAlumnos: activeStudents.filter(s => s.classroom === staffMember.classroom).length,
    rol: staffMember.role === 'TEACHER' ? 'Docente Titular' : staffMember.role
  }] : [];

  setDocenteData({
    id: staffMember.id,
    nombre: staffMember.firstName || '',
    apellido: staffMember.lastName || '',
    legajo: staffMember.id ? staffMember.id.substring(0, 8) : '-',
    cargo: staffMember.role || 'STAFF',
    estado: 'ACTIVO',
    dni: staffMember.documentNumber || '-',
    email: staffMember.email || '-',
    telefono: staffMember.phone || '-',
    fechaIngreso: staffMember.hireDate || '-',
    cursos: cursosAsignados
  });

  evaluarPermisosBajaStaff();
}

function evaluarPermisosBajaStaff() {
  const btnBaja = document.getElementById('btn-baja-staff');
  if (btnBaja) {
    if (['DIRECTOR', 'ADMINISTRATIVE'].includes(currentSession.role)) {
      btnBaja.classList.remove('hidden');
    } else {
      btnBaja.classList.add('hidden');
    }
  }
}

function calcularTiempoTrabajado(fechaIngresoStr) {
  if (!fechaIngresoStr) return "Tiempo no especificado (sin fecha de ingreso)";

  const fechaIngreso = new Date(fechaIngresoStr);
  const fechaActual = new Date();

  if (isNaN(fechaIngreso.getTime())) return "Fecha de ingreso no disponible";

  let años = fechaActual.getFullYear() - fechaIngreso.getFullYear();
  let meses = fechaActual.getMonth() - fechaIngreso.getMonth();

  if (meses < 0) {
    años--;
    meses += 12;
  }

  if (fechaActual.getDate() < fechaIngreso.getDate()) {
    meses--;
    if (meses < 0) {
      años--;
      meses += 12;
    }
  }

  const totalMeses = (años * 12) + meses;

  if (totalMeses < 12) {
    return `${totalMeses} ${totalMeses === 1 ? 'mes' : 'meses'} (${totalMeses} meses acumulados para liquidación)`;
  } else {
    const restoMeses = meses;
    const strAños = `${años} ${años === 1 ? 'año' : 'años'}`;
    const strMeses = restoMeses > 0 ? ` y ${restoMeses} ${restoMeses === 1 ? 'mes' : 'meses'}` : '';
    return `${strAños}${strMeses} (Total: ${totalMeses} meses de servicio)`;
  }
}

async function confirmarBajaStaff() {
  if (!currentStaffData || !currentStaffData.id) {
    alert("No se pudo identificar la ficha del personal a dar de baja.");
    return;
  }

  const nombreCompleto = `${currentStaffData.firstName || ''} ${currentStaffData.lastName || ''}`.trim();
  const fechaIngreso = currentStaffData.hireDate;

  const tiempoTrabajado = calcularTiempoTrabajado(fechaIngreso);

  const confirmacion = confirm(
      `¿Estás seguro de que deseas dar de baja a "${nombreCompleto}"?\n\n` +
      `📋 Antigüedad registrada: ${tiempoTrabajado}\n` +
      `Fecha de ingreso: ${fechaIngreso || 'No registrada'}\n\n` +
      `Presiona Aceptar para procesar la baja.`
  );

  if (!confirmacion) return;

  try {
    const response = await fetch(`${API_BASE_URL}/institution/staff/${currentStaffData.id}/baja`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      }
    });

    if (response.ok || response.status === 200 || response.status === 204) {
      alert(
          `✅ El docente/personal "${nombreCompleto}" fue dado de baja y movido al registro histórico.\n\n` +
          `📊 RESUMEN PARA CÁLCULOS ECONÓMICOS / LIQUIDACIÓN:\n` +
          `• Empleado: ${nombreCompleto}\n` +
          `• Tiempo trabajado: ${tiempoTrabajado}\n` +
          `• Fecha de ingreso: ${fechaIngreso || 'N/D'}\n` +
          `• Fecha de egreso: ${new Date().toLocaleDateString('es-AR')}`
      );

      hideStaffProfile();
      await fetchStaff();
    } else {
      let errText = "Error desconocido";
      try {
        const errorJson = await response.json();
        errText = errorJson.message || errText;
      } catch (e) {
        errText = await response.text();
      }

      alert(`No se pudo procesar la baja (${response.status}): ${errText}`);
    }

  } catch (error) {
    console.error("Error al procesar baja de personal:", error);
    alert("Hubo un fallo de red o conexión al intentar procesar la baja.");
  }
}

window.setDocenteData = function(data) {
  if (!data) return;

  document.querySelectorAll('.dashboard-view').forEach(v => v.classList.add('hidden'));
  document.getElementById('staffProfileView').classList.remove('hidden');

  const nombreCompleto = `${data.nombre || ''} ${data.apellido || ''}`.trim() || 'Sin Nombre';
  document.getElementById('docente-nombre').textContent = nombreCompleto;
  document.getElementById('docente-avatar').textContent = getInitials(nombreCompleto);
  document.getElementById('docente-legajo').textContent = `Legajo: ${data.legajo || '-'}`;
  document.getElementById('docente-cargo').textContent = `Cargo: ${data.cargo || '-'}`;
  document.getElementById('docente-estado').textContent = `Estado: ${data.estado || 'ACTIVO'}`;
  document.getElementById('docente-dni').textContent = data.dni || '-';
  document.getElementById('docente-email').textContent = data.email || '-';
  document.getElementById('docente-telefono').textContent = data.telefono || '-';
  document.getElementById('docente-ingreso').textContent = data.fechaIngreso || '-';

  const cursosCont = document.getElementById('cursos-container');
  cursosCont.innerHTML = '';

  if (data.cursos && data.cursos.length > 0) {
    data.cursos.forEach(c => {
      const item = document.createElement('div');
      item.className = 'course-card bg-slate-50 border border-slate-200 rounded-lg p-3';
      item.innerHTML = `
        <h4 class="font-bold text-slate-800 text-sm">${c.nombre}</h4>
        <p class="text-xs text-slate-500 mt-1">${c.turno} | Alumnos: ${c.cantidadAlumnos}</p>
        <span class="bg-teal-100 text-teal-800 text-[10px] font-bold px-2 py-0.5 rounded mt-2 inline-block">${c.rol}</span>
      `;
      cursosCont.appendChild(item);
    });
  } else {
    cursosCont.innerHTML = '<p class="text-xs text-slate-400 col-span-2">No posee asignaturas ni salitas a cargo en el período activo.</p>';
  }
};

function hideStaffProfile() {
  document.getElementById('staffProfileView').classList.add('hidden');
  document.getElementById('staffView').classList.remove('hidden');
}

// ========================================================
// ACCIONES DE PLANILLA Y EDICIÓN DE STAFF
// ========================================================

function verPlanillaClasesDocente() {
  if (!currentStaffData) return;

  const nombre = `${currentStaffData.firstName || ''} ${currentStaffData.lastName || ''}`.trim();
  const salita = currentStaffData.classroom || 'Sin salita asignada';
  const cantAlumnos = activeStudents.filter(s => s.classroom === currentStaffData.classroom).length;

  document.getElementById('planilla-docente-nombre').textContent = nombre;
  document.getElementById('planilla-salita-nombre').textContent = `Salita: ${salita}`;
  document.getElementById('planilla-cant-alumnos').textContent = cantAlumnos;

  document.getElementById('planillaModal').classList.remove('hidden');
}

function cerrarModalPlanilla() {
  document.getElementById('planillaModal').classList.add('hidden');
}

function abrirModalEdicionStaff() {
  if (!currentStaffData) return;

  document.getElementById('edit-staff-nombre').value = currentStaffData.firstName || '';
  document.getElementById('edit-staff-apellido').value = currentStaffData.lastName || '';
  document.getElementById('edit-staff-email').value = currentStaffData.email || '';
  document.getElementById('edit-staff-role').value = currentStaffData.role || 'TEACHER';
  document.getElementById('edit-staff-classroom').value = currentStaffData.classroom || 'Maternal';

  handleStaffRoleFormChange(currentStaffData.role || 'TEACHER', 'editStaffClassroomBlock');

  document.getElementById('staffEditModal').classList.remove('hidden');
}

function cerrarModalEdicionStaff() {
  document.getElementById('staffEditModal').classList.add('hidden');
}

async function guardarDatosStaff(e) {
  e.preventDefault();
  if (!currentStaffData || !currentStaffData.id) return;

  const role = document.getElementById('edit-staff-role').value;
  const updatedPayload = {
    firstName: document.getElementById('edit-staff-nombre').value.trim(),
    lastName: document.getElementById('edit-staff-apellido').value.trim(),
    email: document.getElementById('edit-staff-email').value.trim(),
    role: role,
    classroom: role === 'TEACHER' ? document.getElementById('edit-staff-classroom').value : null
  };

  try {
    const response = await fetch(`${API_BASE_URL}/institution/staff/${currentStaffData.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      },
      body: JSON.stringify(updatedPayload)
    });

    if (!response.ok) throw new Error("Error al actualizar los datos en el servidor");

    const usuarioActualizado = await response.json();
    alert("¡Legajo del docente/personal actualizado con éxito!");

    currentStaffData = usuarioActualizado;
    cerrarModalEdicionStaff();

    await fetchStaff();
    viewStaffProfile(usuarioActualizado.id);

  } catch (error) {
    console.error("Error al actualizar legajo de personal:", error);
    alert("No se pudo actualizar el legajo. Revisa la conexión con el servidor.");
  }
}

// ========================================================
// MÓDULO DE GESTIÓN DE CUOTAS
// ========================================================

const ARANCELES_CICLO_LECTIVO = [
  { id: 0, nombre: 'Matrícula', key: 'MATRICULA' },
  { id: 3, nombre: 'Marzo', key: 'MARZO' },
  { id: 4, nombre: 'Abril', key: 'ABRIL' },
  { id: 5, nombre: 'Mayo', key: 'MAYO' },
  { id: 6, nombre: 'Junio', key: 'JUNIO' },
  { id: 7, nombre: 'Julio', key: 'JULIO' },
  { id: 8, nombre: 'Agosto', key: 'AGOSTO' },
  { id: 9, nombre: 'Septiembre', key: 'SEPTIEMBRE' },
  { id: 10, nombre: 'Octubre', key: 'OCTUBRE' },
  { id: 11, nombre: 'Noviembre', key: 'NOVIEMBRE' },
  { id: 12, nombre: 'Diciembre', key: 'DICIEMBRE' }
];

let isEditingCuotas = false;
let tempFeesState = {};

async function renderCuotasView() {
  await fetchInstitutionalEmails();
  volverAListaCuotas();
  filterCuotasTable();
}

async function fetchInstitutionalEmails() {
  try {
    const res = await fetch(`${API_BASE_URL}/institutions/settings/emails`, {
      headers: {
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      }
    });
    if (res.ok) {
      institutionalEmails = await res.json();
    }
  } catch (e) {
    console.error("Error cargando emails institucionales:", e);
  }
}

function filterCuotasTable() {
  const tbody = document.getElementById('cuotasTableBody');
  if (!tbody) return;

  const filterClassroom = document.getElementById('filterCuotasClassroom')?.value || 'TODAS';
  const rawQuery = document.getElementById('inputFilterCuotasStudents')?.value || '';
  const role = currentSession.role;

  let list = activeStudents;
  if (role === 'TUTOR') {
    list = activeStudents.filter(s => {
      if (s.tutors && s.tutors.some(t => t.email === currentSession.email)) return true;
      return s.tutorEmail === currentSession.email;
    });
  }

  const terms = normalizarTexto(rawQuery).split(/\s+/).filter(t => t.length > 0);

  const filtered = list.filter(s => {
    const matchClass = (filterClassroom === 'TODAS' || s.classroom === filterClassroom);
    if (!matchClass) return false;

    if (terms.length === 0) return true;

    const searchableString = normalizarTexto(`
      ${s.firstName || ''} 
      ${s.lastName || ''} 
      ${s.legajoNumber || s.legajo || ''}
      ${s.documentNumber || ''} 
      ${s.id || ''} 
      ${s.classroom || ''}
    `);

    return terms.every(term => searchableString.includes(term));
  });

  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" class="p-4 text-center text-slate-400 text-xs">No se encontraron alumnos registrados.</td></tr>`;
    return;
  }

  tbody.innerHTML = filtered.map(s => `
    <tr class="hover:bg-slate-50/80 transition-colors">
      <td class="p-3.5 font-semibold text-slate-900">${s.lastName || ''}, ${s.firstName || ''}</td>
      <td class="p-3.5 text-slate-600 font-medium">${s.documentNumber || '--'}</td>
      <td class="p-3.5"><span class="bg-blue-50 text-blue-700 text-xs font-bold px-2.5 py-0.5 rounded-full">${s.classroom || 'Sin asignar'}</span></td>
      <td class="p-3.5 text-center">
        <button onclick="abrirDetalleCuotas('${s.id}')" class="bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 px-3 py-1 rounded-lg font-bold text-xs cursor-pointer transition-all flex items-center gap-1 mx-auto">
          <span class="material-icons-outlined text-sm">payments</span>
          <span>Ver Cuotas</span>
        </button>
      </td>
    </tr>
  `).join('');
}

async function abrirDetalleCuotas(studentId) {
  selectedStudentForCuotas = studentId;
  const alumno = activeStudents.find(s => s.id === studentId);
  if (!alumno) return;

  document.getElementById('cuotasListView').classList.add('hidden');
  document.getElementById('cuotasDetailView').classList.remove('hidden');

  const legajoVisual = alumno.legajoNumber || alumno.legajo || (alumno.id ? alumno.id.substring(0, 8) : '-');

  document.getElementById('cuotasAlumnoNombre').textContent = `${alumno.lastName || ''}, ${alumno.firstName || ''}`;
  document.getElementById('cuotasAlumnoDni').textContent = alumno.documentNumber || '-';
  document.getElementById('cuotasAlumnoLegajoSala').textContent = `Legajo: ${legajoVisual} | Salita: ${alumno.classroom || '-'}`;

  const esAdmin = (currentSession.role === 'DIRECTOR' || currentSession.role === 'ADMINISTRATIVE');
  document.getElementById('cuotasEditActionContainer').classList.toggle('hidden', !esAdmin);
  document.getElementById('btnEditarMailComprobante').style.display = esAdmin ? 'inline-block' : 'none';
  document.getElementById('btnEditarMailConsulta').style.display = esAdmin ? 'inline-block' : 'none';

  const mailCompEl = document.getElementById('linkMailComprobante');
  const mailConsEl = document.getElementById('linkMailConsulta');

  mailCompEl.textContent = institutionalEmails.receiptEmail || 'No configurado';
  mailCompEl.href = `https://mail.google.com/mail/?view=cm&fs=1&to=${encodeURIComponent(institutionalEmails.receiptEmail || '')}&su=${encodeURIComponent(`Comprobante de Pago - ${alumno.lastName}, ${alumno.firstName}`)}`;

  mailConsEl.textContent = institutionalEmails.feeQueryEmail || 'No configurado';
  mailConsEl.href = `https://mail.google.com/mail/?view=cm&fs=1&to=${encodeURIComponent(institutionalEmails.feeQueryEmail || '')}&su=${encodeURIComponent(`Consulta Aranceles - ${alumno.lastName}, ${alumno.firstName}`)}`;

  isEditingCuotas = false;
  await fetchStudentFees(studentId);
  tempFeesState = {};
  studentFeesList.forEach(f => {
    tempFeesState[f.monthNumber] = (f.status === 'PAID');
  });

  dibujarCasillerosCuotas();
}

function volverAListaCuotas() {
  selectedStudentForCuotas = null;
  isEditingCuotas = false;
  document.getElementById('cuotasDetailView').classList.add('hidden');
  document.getElementById('cuotasListView').classList.remove('hidden');
}

async function fetchStudentFees(studentId) {
  try {
    const res = await fetch(`${API_BASE_URL}/students/${studentId}/fees?academicYear=2026`, {
      headers: {
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      }
    });
    studentFeesList = res.ok ? await res.json() : [];
  } catch (e) {
    console.error("Error al consultar aranceles del backend:", e);
    studentFeesList = [];
  }
}

function dibujarCasillerosCuotas() {
  const container = document.getElementById('mesesCuotasContainer');
  if (!container) return;

  const mesActual = new Date().getMonth() + 1;

  container.innerHTML = ARANCELES_CICLO_LECTIVO.map(m => {
    const isPaid = tempFeesState[m.id] === true;
    let estiloBorde = 'border-slate-800 bg-slate-50 text-slate-500';
    let icono = '-';
    let textoEstado = 'Inactivo';

    if (isPaid) {
      estiloBorde = 'border-emerald-600 bg-emerald-50 text-emerald-700';
      icono = '✓';
      textoEstado = 'Abonado';
    } else {
      if (m.id === 0 || m.id <= mesActual) {
        estiloBorde = 'border-rose-600 bg-rose-50 text-rose-700';
        icono = '!';
        textoEstado = 'Pendiente';
      }
    }

    const cursorStyle = isEditingCuotas ? 'cursor-pointer hover:scale-105 hover:ring-2 hover:ring-emerald-500 shadow-xs' : 'cursor-default opacity-90';

    return `
      <div onclick="${isEditingCuotas ? `clickCasilleroEdicion(${m.id})` : ''}" 
           class="p-2.5 rounded-xl border-2 ${estiloBorde} ${cursorStyle} transition-all flex flex-col items-center justify-between text-center select-none min-h-[96px]">
        <span class="text-[11px] font-bold text-slate-700 uppercase tracking-tight">${m.nombre}</span>
        <div class="w-7 h-7 rounded-lg border-2 flex items-center justify-center font-bold text-sm ${estiloBorde}">
          ${icono}
        </div>
        <span class="text-[10px] font-semibold">${textoEstado}</span>
      </div>
    `;
  }).join('');
}

function habilitarModoEdicionCuotas() {
  isEditingCuotas = true;
  document.getElementById('btnHabilitarEdicionCuotas').classList.add('hidden');
  document.getElementById('btnGuardarEdicionCuotas').classList.remove('hidden');
  document.getElementById('btnCancelarEdicionCuotas').classList.remove('hidden');
  document.getElementById('badgeModoEdicion').classList.remove('hidden');
  dibujarCasillerosCuotas();
}

function cancelarModoEdicionCuotas() {
  isEditingCuotas = false;
  tempFeesState = {};
  studentFeesList.forEach(f => {
    tempFeesState[f.monthNumber] = (f.status === 'PAID');
  });
  document.getElementById('btnHabilitarEdicionCuotas').classList.remove('hidden');
  document.getElementById('btnGuardarEdicionCuotas').classList.add('hidden');
  document.getElementById('btnCancelarEdicionCuotas').classList.add('hidden');
  document.getElementById('badgeModoEdicion').classList.add('hidden');
  dibujarCasillerosCuotas();
}

function clickCasilleroEdicion(mesId) {
  tempFeesState[mesId] = !tempFeesState[mesId];
  dibujarCasillerosCuotas();
}

async function guardarCambiosCuotas() {
  if (!selectedStudentForCuotas) return;

  try {
    for (const m of ARANCELES_CICLO_LECTIVO) {
      const dbFee = studentFeesList.find(f => f.monthNumber === m.id);
      const isPaidCurrently = dbFee ? (dbFee.status === 'PAID') : false;
      const willBePaid = tempFeesState[m.id] === true;

      if (isPaidCurrently !== willBePaid) {
        await fetch(`${API_BASE_URL}/students/${selectedStudentForCuotas}/fees/toggle?academicYear=2026&monthNumber=${m.id}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-Institution-Id': currentSession.institutionId,
            'X-User-Role': currentSession.role
          }
        });
      }
    }

    alert("¡Cuotas del alumno actualizadas con éxito en la base de datos!");
    isEditingCuotas = false;
    document.getElementById('btnHabilitarEdicionCuotas').classList.remove('hidden');
    document.getElementById('btnGuardarEdicionCuotas').classList.add('hidden');
    document.getElementById('btnCancelarEdicionCuotas').classList.add('hidden');
    document.getElementById('badgeModoEdicion').classList.add('hidden');

    await fetchStudentFees(selectedStudentForCuotas);
    dibujarCasillerosCuotas();

  } catch (error) {
    console.error("Error al guardar cambios de cuotas:", error);
    alert("Hubo un problema al guardar las cuotas en el servidor.");
  }
}

function editarMailCuotas(tipo) {
  document.getElementById('tipoEmailEditando').value = tipo;
  const inputEmail = document.getElementById('inputModalEmail');
  const titulo = document.getElementById('modalEmailTitulo');
  const label = document.getElementById('lblModalEmail');

  if (tipo === 'comprobante') {
    titulo.innerHTML = '<span class="material-icons-outlined">receipt</span> Modificar Mail Institucional de Comprobantes';
    label.textContent = 'Correo Oficial para Recepción de Comprobantes';
    inputEmail.value = institutionalEmails.receiptEmail || '';
  } else {
    titulo.innerHTML = '<span class="material-icons-outlined">mail</span> Modificar Mail Institucional de Consultas';
    label.textContent = 'Correo Oficial para Consultas de Aranceles';
    inputEmail.value = institutionalEmails.feeQueryEmail || '';
  }

  document.getElementById('cuotasEmailModal').classList.remove('hidden');
}

function cerrarModalEmailCuotas() {
  document.getElementById('cuotasEmailModal').classList.add('hidden');
}

async function guardarEmailCuotas(e) {
  e.preventDefault();
  const tipo = document.getElementById('tipoEmailEditando').value;
  const nuevoMail = document.getElementById('inputModalEmail').value.trim();

  const payload = {
    receiptEmail: tipo === 'comprobante' ? nuevoMail : institutionalEmails.receiptEmail,
    feeQueryEmail: tipo === 'consulta' ? nuevoMail : institutionalEmails.feeQueryEmail
  };

  try {
    const res = await fetch(`${API_BASE_URL}/institutions/settings/emails`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      alert("¡Correo institucional actualizado correctamente!");
      institutionalEmails = payload;
      cerrarModalEmailCuotas();
      if (selectedStudentForCuotas) {
        abrirDetalleCuotas(selectedStudentForCuotas);
      }
    } else {
      alert("No se pudo actualizar el correo en el servidor.");
    }
  } catch (error) {
    console.error("Error al guardar email:", error);
    alert("Error de conexión con el servidor.");
  }
}

// ========================================================
// MÓDULO DE CONTROL DE RETIROS DE ALUMNOS (AUTORIZADOS)
// ========================================================

function renderRetirosView() {
  filterRetirosTable();
}

function filterRetirosTable() {
  const container = document.getElementById('retirosAlumnosList');
  if (!container) return;

  const filterClassroom = document.getElementById('filterRetirosClassroom')?.value || 'TODAS';
  const rawQuery = document.getElementById('inputFilterRetiros')?.value || '';

  const terms = normalizarTexto(rawQuery).split(/\s+/).filter(t => t.length > 0);

  const filtrados = activeStudents.filter(s => {
    const matchClass = (filterClassroom === 'TODAS' || s.classroom === filterClassroom);
    if (!matchClass) return false;

    if (terms.length === 0) return true;

    const searchableString = normalizarTexto(`
      ${s.firstName || ''} 
      ${s.lastName || ''} 
      ${s.legajoNumber || s.legajo || ''}
      ${s.documentNumber || ''} 
      ${s.id || ''} 
      ${s.classroom || ''}
    `);

    return terms.every(term => searchableString.includes(term));
  });

  if (filtrados.length === 0) {
    container.innerHTML = '<p class="text-xs text-slate-400 p-4 text-center">No se encontraron alumnos.</p>';
    return;
  }

  container.innerHTML = filtrados.map(s => `
    <div onclick="seleccionarAlumnoRetiros('${s.id}')" class="p-3 hover:bg-emerald-50/60 cursor-pointer flex justify-between items-center transition-colors ${selectedStudentForRetiros === s.id ? 'bg-emerald-50 border-l-4 border-emerald-600' : ''}">
      <div>
        <h4 class="font-bold text-slate-800 text-xs">${s.lastName || ''}, ${s.firstName || ''}</h4>
        <span class="text-slate-400 text-[11px] block">DNI: ${s.documentNumber || '--'} | ${s.classroom || 'Sin sala'}</span>
      </div>
      <span class="material-icons-outlined text-slate-300 text-sm">chevron_right</span>
    </div>
  `).join('');
}

async function seleccionarAlumnoRetiros(studentId) {
  selectedStudentForRetiros = studentId;
  const alumno = activeStudents.find(s => s.id === studentId);
  if (!alumno) return;

  const legajoVisual = alumno.legajoNumber || alumno.legajo || (alumno.id ? alumno.id.substring(0, 8) : '--');

  document.getElementById('retirosAlumnoNombreHeader').textContent = `${alumno.lastName || ''}, ${alumno.firstName || ''}`;
  document.getElementById('retirosAlumnoInfoSub').textContent = `DNI: ${alumno.documentNumber || '--'} | Salita: ${alumno.classroom || '--'} | Legajo: ${legajoVisual}`;
  document.getElementById('btnAgregarAutorizadoPanel').classList.remove('hidden');

  filterRetirosTable();
  await fetchAndRenderPickupsGeneral(studentId, 'retirosDetalleAutorizados');
}

function calcularEdadDesdeFecha(fechaStr) {
  if (!fechaStr) return null;
  const hoy = new Date();
  const cumple = new Date(fechaStr + 'T00:00:00');
  let edad = hoy.getFullYear() - cumple.getFullYear();
  const m = hoy.getMonth() - cumple.getMonth();
  if (m < 0 || (m === 0 && hoy.getDate() < cumple.getDate())) {
    edad--;
  }
  return edad;
}

function actualizarEdadVisual() {
  const inputDate = document.getElementById('pickupBirthDate');
  const lbl = document.getElementById('labelEdadCalculada');
  if (!inputDate || !lbl) return;

  if (!inputDate.value) {
    lbl.textContent = "Seleccione fecha";
    lbl.className = "font-bold text-slate-400";
    return;
  }

  const edad = calcularEdadDesdeFecha(inputDate.value);
  if (edad < 18) {
    lbl.textContent = `${edad} años (❌ No permitido: Menor de 18)`;
    lbl.className = "font-bold text-rose-600";
  } else {
    lbl.textContent = `${edad} años (✓ Mayor de edad)`;
    lbl.className = "font-bold text-emerald-700";
  }
}

async function fetchAndRenderPickupsGeneral(studentId, targetContainerId) {
  const container = document.getElementById(targetContainerId);
  if (!container) return;

  try {
    const res = await fetch(`${API_BASE_URL}/students/${studentId}/authorized-pickups`, {
      headers: {
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      }
    });

    currentStudentPickups = res.ok ? await res.json() : [];

    if (currentStudentPickups.length === 0) {
      container.innerHTML = `
        <div class="p-4 bg-slate-50 border border-dashed border-slate-200 rounded-xl text-center text-xs text-slate-400">
          No hay personas autorizadas registradas para este alumno.
        </div>
      `;
      return;
    }

    container.innerHTML = currentStudentPickups.map(p => `
      <div onclick="verPerfilAmpliadoAutorizado('${p.id}')" class="p-3.5 bg-white border border-slate-200 rounded-xl shadow-xs hover:border-emerald-500 hover:shadow-sm transition-all flex justify-between items-center cursor-pointer group">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold text-sm group-hover:bg-emerald-600 group-hover:text-white transition-colors">
            ${getInitials(p.fullName)}
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h4 class="font-bold text-slate-900 group-hover:text-emerald-700 text-xs">${p.fullName}</h4>
              <span class="bg-emerald-50 text-emerald-700 border border-emerald-200 text-[10px] font-bold px-2 py-0.5 rounded-md">${p.relationship}</span>
            </div>
            <p class="text-[11px] text-slate-500 mt-0.5">
              DNI: ${p.documentNumber} | ${p.age} años | Tel: ${p.phone}
            </p>
          </div>
        </div>

        <div class="flex items-center gap-1">
          <button onclick="event.stopPropagation(); abrirModalEditarAutorizado('${p.id}')" title="Editar" class="p-1.5 text-slate-400 hover:text-emerald-600 rounded-lg hover:bg-slate-100 cursor-pointer">
            <span class="material-icons-outlined text-sm">edit</span>
          </button>
          <button onclick="event.stopPropagation(); eliminarPersonaAutorizada('${p.id}')" title="Eliminar" class="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50 cursor-pointer">
            <span class="material-icons-outlined text-sm">delete</span>
          </button>
        </div>
      </div>
    `).join('');

  } catch (error) {
    console.error("Error al cargar autorizados:", error);
  }
}

function verPerfilAmpliadoAutorizado(pickupId) {
  const p = currentStudentPickups.find(item => item.id === pickupId);
  if (!p) return;

  document.getElementById('detailPickupAvatar').textContent = getInitials(p.fullName);
  document.getElementById('detailPickupName').textContent = p.fullName;
  document.getElementById('detailPickupRel').textContent = p.relationship;
  document.getElementById('detailPickupDni').textContent = p.documentNumber;
  document.getElementById('detailPickupAge').textContent = `${p.age} años ${p.birthDate ? '(' + p.birthDate + ')' : ''}`;
  document.getElementById('detailPickupPhone').textContent = p.phone;

  document.getElementById('detailPickupActions').innerHTML = `
    <button onclick="cerrarModalDetalleAutorizado(); abrirModalEditarAutorizado('${p.id}')" class="px-4 py-2 bg-emerald-600 text-white rounded-lg text-xs font-semibold hover:bg-emerald-700 cursor-pointer flex items-center gap-1">
      <span class="material-icons-outlined text-xs">edit</span>
      Editar Ficha
    </button>
    <button onclick="cerrarModalDetalleAutorizado()" class="px-4 py-2 border border-slate-200 rounded-lg text-xs font-semibold hover:bg-slate-50 cursor-pointer">Cerrar</button>
  `;

  document.getElementById('pickupDetailModal').classList.remove('hidden');
}

function cerrarModalDetalleAutorizado() {
  document.getElementById('pickupDetailModal').classList.add('hidden');
}

function abrirModalNuevoAutorizado() {
  const form = document.getElementById('formAuthorizedPickup');
  if (form) form.reset();

  const editId = document.getElementById('pickupEditId');
  if (editId) editId.value = '';

  const label = document.getElementById('labelEdadCalculada');
  if (label) {
    label.textContent = 'Seleccione fecha';
    label.className = 'font-bold text-slate-400';
  }

  const titulo = document.getElementById('pickupModalTitulo');
  if (titulo) titulo.innerHTML = '<span class="material-icons-outlined">how_to_reg</span> Nueva Persona Autorizada';

  document.getElementById('authorizedPickupModal').classList.remove('hidden');
}

function abrirModalEditarAutorizado(pickupId) {
  const p = currentStudentPickups.find(item => item.id === pickupId);
  if (!p) return;

  document.getElementById('pickupEditId').value = p.id;
  document.getElementById('pickupFullName').value = p.fullName || '';
  document.getElementById('pickupDni').value = p.documentNumber || '';
  document.getElementById('pickupBirthDate').value = p.birthDate || '';
  document.getElementById('pickupRelationship').value = p.relationship || '';
  document.getElementById('pickupPhone').value = p.phone || '';

  actualizarEdadVisual();

  const titulo = document.getElementById('pickupModalTitulo');
  if (titulo) titulo.innerHTML = '<span class="material-icons-outlined">edit</span> Editar Persona Autorizada';

  document.getElementById('authorizedPickupModal').classList.remove('hidden');
}

function cerrarModalNuevoAutorizado() {
  document.getElementById('authorizedPickupModal').classList.add('hidden');
}

async function guardarPersonaAutorizada(e) {
  e.preventDefault();

  let studentId = selectedStudentForRetiros;
  if (!studentId) {
    studentId = currentStudentData?.id;
  }

  if (!studentId || studentId === '-') {
    alert("No se pudo identificar al alumno.");
    return;
  }

  const inputFullName = document.getElementById('pickupFullName');
  const inputDni = document.getElementById('pickupDni');
  const inputBirthDate = document.getElementById('pickupBirthDate');
  const inputRelationship = document.getElementById('pickupRelationship');
  const inputPhone = document.getElementById('pickupPhone');
  const inputEditId = document.getElementById('pickupEditId');

  if (!inputFullName || !inputDni || !inputBirthDate || !inputRelationship || !inputPhone) {
    console.error("Faltan inputs en el modal de autorizado.");
    return;
  }

  const birthDateValue = inputBirthDate.value;
  const edadCalculada = calcularEdadDesdeFecha(birthDateValue);

  if (edadCalculada === null || isNaN(edadCalculada)) {
    alert("Por favor ingrese una fecha de nacimiento válida.");
    return;
  }

  if (edadCalculada < 18) {
    alert(`La persona autorizada tiene ${edadCalculada} años. Debe ser mayor de 18 años según la normativa (Art. 154).`);
    return;
  }

  const editId = inputEditId ? inputEditId.value.trim() : '';
  const payload = {
    fullName: inputFullName.value.trim(),
    documentNumber: inputDni.value.trim(),
    birthDate: birthDateValue,
    age: edadCalculada,
    relationship: inputRelationship.value.trim(),
    phone: inputPhone.value.trim()
  };

  try {
    const url = editId
        ? `${API_BASE_URL}/students/${studentId}/authorized-pickups/${editId}`
        : `${API_BASE_URL}/students/${studentId}/authorized-pickups`;

    const method = editId ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method: method,
      headers: {
        'Content-Type': 'application/json',
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      alert("¡Persona autorizada guardada correctamente!");
      cerrarModalNuevoAutorizado();
      if (document.getElementById('studentProfileView') && !document.getElementById('studentProfileView').classList.contains('hidden')) {
        await fetchAndRenderPickupsGeneral(studentId, 'autorizados-container');
      }
      if (document.getElementById('retirosView') && !document.getElementById('retirosView').classList.contains('hidden')) {
        await fetchAndRenderPickupsGeneral(studentId, 'retirosDetalleAutorizados');
      }
    } else {
      const errText = await res.text();
      alert(`Error al guardar: ${errText}`);
    }
  } catch (err) {
    console.error("Error al guardar persona autorizada:", err);
    alert("Error de conexión con el servidor.");
  }
}

async function eliminarPersonaAutorizada(pickupId) {
  let studentId = selectedStudentForRetiros;
  if (!studentId) {
    studentId = currentStudentData?.id;
  }

  if (!confirm("¿Deseas retirar la autorización de retiro a esta persona?")) return;

  try {
    const res = await fetch(`${API_BASE_URL}/students/${studentId}/authorized-pickups/${pickupId}`, {
      method: 'DELETE',
      headers: {
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      }
    });

    if (res.ok) {
      if (document.getElementById('studentProfileView') && !document.getElementById('studentProfileView').classList.contains('hidden')) {
        await fetchAndRenderPickupsGeneral(studentId, 'autorizados-container');
      }
      if (document.getElementById('retirosView') && !document.getElementById('retirosView').classList.contains('hidden')) {
        await fetchAndRenderPickupsGeneral(studentId, 'retirosDetalleAutorizados');
      }
    } else {
      alert("No se pudo eliminar la autorización.");
    }
  } catch (err) {
    console.error("Error al eliminar autorizado:", err);
  }
}

// ========================================================
// MÓDULO DE RESTRICCIONES JUDICIALES
// ========================================================

function renderRestriccionesView() {
  filterRestriccionesTable();
}

function filterRestriccionesTable() {
  const container = document.getElementById('restriccionesAlumnosList');
  if (!container) return;

  const filterClassroom = document.getElementById('filterRestriccionesClassroom')?.value || 'TODAS';
  const rawQuery = document.getElementById('inputFilterRestricciones')?.value || '';

  const terms = normalizarTexto(rawQuery).split(/\s+/).filter(t => t.length > 0);

  const filtrados = activeStudents.filter(s => {
    const matchClass = (filterClassroom === 'TODAS' || s.classroom === filterClassroom);
    if (!matchClass) return false;

    if (terms.length === 0) return true;

    const searchableString = normalizarTexto(`
      ${s.firstName || ''} 
      ${s.lastName || ''} 
      ${s.legajoNumber || s.legajo || ''}
      ${s.documentNumber || ''} 
      ${s.id || ''} 
      ${s.classroom || ''}
    `);

    return terms.every(term => searchableString.includes(term));
  });

  if (filtrados.length === 0) {
    container.innerHTML = '<p class="text-xs text-slate-400 p-4 text-center">No se encontraron alumnos.</p>';
    return;
  }

  container.innerHTML = filtrados.map(s => `
    <div onclick="seleccionarAlumnoRestricciones('${s.id}')" class="p-3 hover:bg-rose-50/60 cursor-pointer flex justify-between items-center transition-colors ${selectedStudentForRestricciones === s.id ? 'bg-rose-50 border-l-4 border-rose-600' : ''}">
      <div>
        <h4 class="font-bold text-slate-800 text-xs">${s.lastName || ''}, ${s.firstName || ''}</h4>
        <span class="text-slate-400 text-[11px] block">DNI: ${s.documentNumber || '--'} | ${s.classroom || 'Sin sala'}</span>
      </div>
      <span class="material-icons-outlined text-slate-300 text-sm">chevron_right</span>
    </div>
  `).join('');
}

async function seleccionarAlumnoRestricciones(studentId) {
  selectedStudentForRestricciones = studentId;
  const alumno = activeStudents.find(s => s.id === studentId);
  if (!alumno) return;

  const legajoVisual = alumno.legajoNumber || alumno.legajo || (alumno.id ? alumno.id.substring(0, 8) : '--');

  const header = document.getElementById('restriccionesAlumnoNombreHeader');
  const sub = document.getElementById('restriccionesAlumnoInfoSub');
  const btn = document.getElementById('btnAgregarRestriccionPanel');

  if (header) header.textContent = `${alumno.lastName || ''}, ${alumno.firstName || ''}`;
  if (sub) sub.textContent = `DNI: ${alumno.documentNumber || '--'} | Salita: ${alumno.classroom || '--'} | Legajo: ${legajoVisual}`;
  if (btn) btn.classList.remove('hidden');

  filterRestriccionesTable();
  await fetchAndRenderRestrictionsGeneral(studentId, 'restriccionesDetalleList');
}

async function fetchAndRenderRestrictionsGeneral(studentId, targetContainerId) {
  const container = document.getElementById(targetContainerId);
  if (!container) return;

  try {
    const res = await fetch(`${API_BASE_URL}/students/${studentId}/judicial-restrictions`, {
      headers: {
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      }
    });

    currentStudentRestrictions = res.ok ? await res.json() : [];

    if (currentStudentRestrictions.length === 0) {
      container.innerHTML = `
        <div class="p-4 bg-slate-50 border border-dashed border-slate-200 rounded-xl text-center text-xs text-slate-400">
          No constan medidas de restricción judicial certificadas para este alumno.
        </div>
      `;
      return;
    }

    container.innerHTML = currentStudentRestrictions.map(r => `
      <div onclick="verPerfilAmpliadoRestriccion('${r.id}')" class="p-3.5 bg-white border border-rose-200 rounded-xl shadow-xs hover:border-rose-500 hover:shadow-sm transition-all flex justify-between items-center cursor-pointer group">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-rose-100 text-rose-700 flex items-center justify-center font-bold text-sm group-hover:bg-rose-600 group-hover:text-white transition-colors">
            ${getInitials(`${r.firstName || ''} ${r.lastName || ''}`)}
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h4 class="font-bold text-slate-900 group-hover:text-rose-700 text-xs">${r.lastName}, ${r.firstName}</h4>
              <span class="bg-rose-50 text-rose-700 border border-rose-200 text-[10px] font-bold px-2 py-0.5 rounded-md">Prohibición Judicial</span>
            </div>
            <p class="text-[11px] text-slate-500 mt-0.5">
              ${r.documentType || 'DNI'}: ${r.documentNumber} | <strong>Medida:</strong> ${r.description.length > 45 ? r.description.substring(0, 45) + '...' : r.description}
            </p>
          </div>
        </div>

        <div class="flex items-center gap-1">
          <button onclick="event.stopPropagation(); abrirModalEditarRestriccion('${r.id}')" title="Editar" class="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-slate-100 cursor-pointer">
            <span class="material-icons-outlined text-sm">edit</span>
          </button>
          <button onclick="event.stopPropagation(); eliminarRestriccionJudicial('${r.id}')" title="Eliminar" class="p-1.5 text-slate-400 hover:text-rose-700 rounded-lg hover:bg-rose-50 cursor-pointer">
            <span class="material-icons-outlined text-sm">delete</span>
          </button>
        </div>
      </div>
    `).join('');

  } catch (error) {
    console.error("Error cargando restricciones judiciales:", error);
  }
}

function verPerfilAmpliadoRestriccion(restrictionId) {
  const r = currentStudentRestrictions.find(item => item.id === restrictionId);
  if (!r) return;

  const nombreCompleto = `${r.lastName || ''}, ${r.firstName || ''}`.trim();

  const avatarEl = document.getElementById('detailRestrAvatar');
  const nameEl = document.getElementById('detailRestrName');
  const docEl = document.getElementById('detailRestrDoc');
  const dateEl = document.getElementById('detailRestrDate');
  const descEl = document.getElementById('detailRestrDesc');
  const legajoEl = document.getElementById('detailRestrLegajo');
  const matrixEl = document.getElementById('detailRestrMatrix');
  const folioEl = document.getElementById('detailRestrFolio');
  const actionsEl = document.getElementById('detailRestrActions');
  const modalEl = document.getElementById('restrictionDetailModal');

  if (avatarEl) avatarEl.textContent = getInitials(nombreCompleto);
  if (nameEl) nameEl.textContent = nombreCompleto || '-';
  if (docEl) docEl.textContent = `${r.documentType || 'DNI'}: ${r.documentNumber || '-'}`;
  if (dateEl) dateEl.textContent = r.inscriptionDate || 'No especificada';
  if (descEl) descEl.textContent = r.description || '-';
  if (legajoEl) legajoEl.textContent = r.legajoNumber || '-';
  if (matrixEl) matrixEl.textContent = r.matrixNumber || '-';
  if (folioEl) folioEl.textContent = r.folioNumber || '-';

  if (actionsEl) {
    actionsEl.innerHTML = `
      <button onclick="cerrarModalDetalleRestriccion(); abrirModalEditarRestriccion('${r.id}')" class="px-4 py-2 bg-rose-600 text-white rounded-lg text-xs font-semibold hover:bg-rose-700 cursor-pointer flex items-center gap-1">
        <span class="material-icons-outlined text-xs">edit</span>
        Editar Medida
      </button>
      <button type="button" onclick="cerrarModalDetalleRestriccion()" class="px-4 py-2 border border-slate-200 rounded-lg text-xs font-semibold hover:bg-slate-50 cursor-pointer">Cerrar</button>
    `;
  }

  if (modalEl) modalEl.classList.remove('hidden');
}

function cerrarModalDetalleRestriccion() {
  const modal = document.getElementById('restrictionDetailModal');
  if (modal) modal.classList.add('hidden');
}

function abrirModalNuevaRestriccion() {
  const form = document.getElementById('formJudicialRestriction');
  if (form) form.reset();

  const editId = document.getElementById('restriccionEditId');
  if (editId) editId.value = '';

  let student = null;
  if (selectedStudentForRestricciones) {
    student = activeStudents.find(s => s.id === selectedStudentForRestricciones);
  }
  if (!student && currentStudentData) {
    student = currentStudentData;
  }

  const inputLegajo = document.getElementById('restriccionLegajo');
  if (inputLegajo) {
    const legajoVal = student ? (student.legajoNumber || student.legajo || (student.id ? student.id.substring(0, 8) : '')) : '';
    inputLegajo.value = legajoVal;
  }

  const inputDate = document.getElementById('restriccionDate');
  if (inputDate && !inputDate.value) {
    inputDate.value = new Date().toISOString().split('T')[0];
  }

  const titulo = document.getElementById('modalRestriccionTitulo');
  if (titulo) titulo.innerHTML = '<span class="material-icons-outlined">gavel</span> Nueva Restricción Judicial';

  const modal = document.getElementById('judicialRestrictionModal');
  if (modal) modal.classList.remove('hidden');
}

function abrirModalEditarRestriccion(restrictionId) {
  const r = currentStudentRestrictions.find(item => item.id === restrictionId);
  if (!r) return;

  document.getElementById('restriccionEditId').value = r.id;
  document.getElementById('restriccionLastName').value = r.lastName || '';
  document.getElementById('restriccionFirstName').value = r.firstName || '';
  document.getElementById('restriccionDocType').value = r.documentType || 'DNI';
  document.getElementById('restriccionDocNumber').value = r.documentNumber || '';
  document.getElementById('restriccionDescription').value = r.description || '';
  document.getElementById('restriccionLegajo').value = r.legajoNumber || '';
  document.getElementById('restriccionMatrix').value = r.matrixNumber || '';
  document.getElementById('restriccionFolio').value = r.folioNumber || '';
  document.getElementById('restriccionDate').value = r.inscriptionDate || '';

  const titulo = document.getElementById('modalRestriccionTitulo');
  if (titulo) titulo.innerHTML = '<span class="material-icons-outlined">edit</span> Editar Restricción Judicial';

  const modal = document.getElementById('judicialRestrictionModal');
  if (modal) modal.classList.remove('hidden');
}

function cerrarModalRestriccion() {
  const modal = document.getElementById('judicialRestrictionModal');
  if (modal) modal.classList.add('hidden');
}

async function guardarRestriccionJudicial(e) {
  e.preventDefault();

  let studentId = selectedStudentForRestricciones;
  if (!studentId) {
    studentId = currentStudentData?.id;
  }

  if (!studentId || studentId === '-') {
    alert("No se pudo identificar al alumno.");
    return;
  }

  const editId = document.getElementById('restriccionEditId').value.trim();
  const payload = {
    lastName: document.getElementById('restriccionLastName').value.trim(),
    firstName: document.getElementById('restriccionFirstName').value.trim(),
    documentType: document.getElementById('restriccionDocType').value,
    documentNumber: document.getElementById('restriccionDocNumber').value.trim(),
    description: document.getElementById('restriccionDescription').value.trim(),
    legajoNumber: document.getElementById('restriccionLegajo').value.trim(),
    matrixNumber: document.getElementById('restriccionMatrix').value.trim(),
    folioNumber: document.getElementById('restriccionFolio').value.trim(),
    inscriptionDate: document.getElementById('restriccionDate').value || null
  };

  try {
    const url = editId
        ? `${API_BASE_URL}/students/${studentId}/judicial-restrictions/${editId}`
        : `${API_BASE_URL}/students/${studentId}/judicial-restrictions`;

    const method = editId ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method: method,
      headers: {
        'Content-Type': 'application/json',
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      alert("¡Medida de restricción judicial guardada correctamente!");
      cerrarModalRestriccion();
      if (document.getElementById('studentProfileView') && !document.getElementById('studentProfileView').classList.contains('hidden')) {
        await fetchAndRenderRestrictionsGeneral(studentId, 'restricciones-container');
      }
      if (document.getElementById('restriccionesView') && !document.getElementById('restriccionesView').classList.contains('hidden')) {
        await fetchAndRenderRestrictionsGeneral(studentId, 'restriccionesDetalleList');
      }
    } else {
      const errText = await res.text();
      alert(`Error al guardar la restricción: ${errText}`);
    }
  } catch (err) {
    console.error("Error al guardar restricción:", err);
    alert("Error de conexión con el servidor.");
  }
}

async function eliminarRestriccionJudicial(restrictionId) {
  let studentId = selectedStudentForRestricciones;
  if (!studentId) {
    studentId = currentStudentData?.id;
  }

  if (!confirm("¿Está seguro de eliminar este registro de restricción judicial del alumno?")) return;

  try {
    const res = await fetch(`${API_BASE_URL}/students/${studentId}/judicial-restrictions/${restrictionId}`, {
      method: 'DELETE',
      headers: {
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      }
    });

    if (res.ok) {
      if (document.getElementById('studentProfileView') && !document.getElementById('studentProfileView').classList.contains('hidden')) {
        await fetchAndRenderRestrictionsGeneral(studentId, 'restricciones-container');
      }
      if (document.getElementById('restriccionesView') && !document.getElementById('restriccionesView').classList.contains('hidden')) {
        await fetchAndRenderRestrictionsGeneral(studentId, 'restriccionesDetalleList');
      }
    } else {
      alert("No se pudo eliminar el registro de restricción.");
    }
  } catch (err) {
    console.error("Error al eliminar restricción:", err);
  }
}

// ========================================================
// MÓDULO DE COMUNICADOS
// ========================================================

let announcementsList = [];

function renderComunicadosView() {
  filterComunicadosFeed();
  evaluarPermisosComunicados();
}

async function fetchAnnouncements() {
  try {
    const res = await fetch(`${API_BASE_URL}/announcements`, {
      headers: {
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      }
    });

    announcementsList = res.ok ? await res.json() : [];
    renderInicioFeed();
    filterComunicadosFeed();

  } catch (error) {
    console.error("Error al cargar comunicados:", error);
  }
}

function getAuthorizedAnnouncements() {
  let list = announcementsList;

  if (currentSession.role === 'TEACHER') {
    const userClass = currentStaffData?.classroom;
    list = list.filter(a => a.scope === 'GLOBAL' || a.targetClassroom === userClass);
  } else if (currentSession.role === 'TUTOR') {
    const tutorKids = activeStudents.filter(s => s.tutors && s.tutors.some(t => t.email === currentSession.email));
    const kidsSalas = tutorKids.map(k => k.classroom);
    const kidsIds = tutorKids.map(k => k.id);

    list = list.filter(a =>
        a.scope === 'GLOBAL' ||
        (a.scope === 'CLASSROOM' && kidsSalas.includes(a.targetClassroom)) ||
        (a.scope === 'PRIVATE_STUDENT' && kidsIds.includes(a.targetStudentId))
    );
  }
  return list;
}

function renderInicioFeed() {
  const container = document.getElementById('inicioComunicadosFeed');
  if (!container) return;

  const list = getAuthorizedAnnouncements().filter(a => a.scope !== 'PRIVATE_STUDENT').slice(0, 3);

  if (list.length === 0) {
    container.innerHTML = `<div class="p-4 bg-white border border-dashed border-slate-200 rounded-xl text-center text-xs text-slate-400">No hay comunicados recientes publicados.</div>`;
    return;
  }

  container.innerHTML = list.map(a => renderCardHtml(a, false)).join('');
}

function filterComunicadosFeed() {
  const container = document.getElementById('comunicadosFeedContainer');
  if (!container) return;

  const targetScope = document.getElementById('filterComunicadosClassroom')?.value || 'TODAS';
  let list = getAuthorizedAnnouncements().filter(a => a.scope !== 'PRIVATE_STUDENT');

  if (targetScope !== 'TODAS') {
    if (targetScope === 'GLOBAL') {
      list = list.filter(a => a.scope === 'GLOBAL');
    } else {
      list = list.filter(a => a.targetClassroom === targetScope);
    }
  }

  if (list.length === 0) {
    container.innerHTML = `<div class="bg-white border border-dashed border-slate-200 rounded-2xl p-8 text-center text-xs text-slate-400">No hay comunicados publicados para este criterio.</div>`;
    return;
  }

  container.innerHTML = list.map(a => renderCardHtml(a, true)).join('');
}

async function renderComunicadosPrivadosAlumno(studentId) {
  const container = document.getElementById('alumno-comunicados-privados');
  if (!container) return;

  const list = announcementsList.filter(a => a.scope === 'PRIVATE_STUDENT' && a.targetStudentId === studentId);

  if (list.length === 0) {
    container.innerHTML = `<div class="p-3 bg-slate-50 border border-dashed border-slate-200 rounded-xl text-center text-xs text-slate-400">No hay mensajes privados registrados para este alumno.</div>`;
    return;
  }

  container.innerHTML = list.map(a => renderCardHtml(a, true)).join('');
}

function renderCardHtml(a, canEditIfAuthorized) {
  let badgeClass = 'bg-indigo-50 text-indigo-700 border-indigo-200';
  if (a.category === 'ACTIVIDAD') badgeClass = 'bg-emerald-50 text-emerald-700 border-emerald-200';
  if (a.category === 'URGENTE') badgeClass = 'bg-rose-50 text-rose-700 border-rose-200';
  if (a.category === 'PRIVADO') badgeClass = 'bg-amber-50 text-amber-700 border-amber-200';

  const canManage = canEditIfAuthorized && ['DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER'].includes(currentSession.role);

  return `
    <div class="bg-white border border-slate-200 rounded-xl p-4 shadow-xs space-y-2.5">
      <div class="flex justify-between items-start">
        <div class="flex items-center gap-2">
          <span class="border text-[10px] font-bold px-2 py-0.5 rounded uppercase ${badgeClass}">${a.category}</span>
          <span class="text-xs font-semibold text-slate-600">${a.scope === 'GLOBAL' ? 'Todo el Jardín' : (a.scope === 'CLASSROOM' ? `Salita: ${a.targetClassroom}` : 'Mensaje Privado')}</span>
        </div>
        ${canManage ? `
          <div class="flex items-center gap-1">
            <button onclick="abrirModalEditarComunicado('${a.id}')" title="Editar" class="p-1 text-slate-400 hover:text-indigo-600 rounded hover:bg-slate-50 cursor-pointer">
              <span class="material-icons-outlined text-sm">edit</span>
            </button>
            <button onclick="eliminarComunicado('${a.id}')" title="Eliminar" class="p-1 text-slate-400 hover:text-rose-600 rounded hover:bg-rose-50 cursor-pointer">
              <span class="material-icons-outlined text-sm">delete</span>
            </button>
          </div>
        ` : ''}
      </div>

      <div>
        <h4 class="text-sm font-bold text-slate-900">${a.title}</h4>
        <p class="text-xs text-slate-600 mt-1 whitespace-pre-line leading-relaxed">${a.content}</p>
      </div>

      ${a.mediaUrl ? `
        <a href="${a.mediaUrl}" target="_blank" class="inline-flex items-center gap-1 bg-slate-50 border border-slate-200 px-2.5 py-1 rounded text-xs font-semibold text-indigo-700 hover:bg-indigo-50 transition-colors">
          <span class="material-icons-outlined text-xs">smart_display</span>
          <span>Ver Material / Enlace</span>
        </a>
      ` : ''}

      <div class="pt-2 border-t border-slate-100 flex justify-between items-center text-[10px] text-slate-400">
        <span>Por: <strong class="text-slate-600">${a.authorName} (${a.authorRole})</strong></span>
        <span>${a.createdAt ? new Date(a.createdAt).toLocaleDateString('es-AR') : 'Reciente'}</span>
      </div>
    </div>
  `;
}

function handleComunicadoScopeChange() {
  const scope = document.getElementById('comunicadoScope')?.value;
  const blockSala = document.getElementById('comunicadoClassroomBlock');
  if (blockSala) blockSala.classList.toggle('hidden', scope !== 'CLASSROOM');
}

function evaluarPermisosComunicados() {
  const btnNuevo = document.getElementById('btnNuevoComunicado');
  if (btnNuevo) {
    const canCreate = ['DIRECTOR', 'ADMINISTRATIVE', 'PRECEPTOR', 'TEACHER'].includes(currentSession.role);
    btnNuevo.classList.toggle('hidden', !canCreate);
  }
}

function abrirModalNuevoComunicado() {
  const form = document.getElementById('formComunicado');
  if (form) form.reset();

  document.getElementById('comunicadoEditId').value = '';
  document.getElementById('comunicadoTargetStudentId').value = '';

  const scopeSel = document.getElementById('comunicadoScope');
  const salaSel = document.getElementById('comunicadoTargetClassroom');

  if (currentSession.role === 'TEACHER') {
    if (scopeSel) {
      scopeSel.value = 'CLASSROOM';
      scopeSel.querySelector('option[value="GLOBAL"]').disabled = true;
      scopeSel.querySelector('option[value="PRIVATE_STUDENT"]').disabled = true;
    }
    if (salaSel && currentStaffData?.classroom) {
      salaSel.value = currentStaffData.classroom;
    }
  } else {
    if (scopeSel) {
      scopeSel.querySelector('option[value="GLOBAL"]').disabled = false;
      scopeSel.querySelector('option[value="PRIVATE_STUDENT"]').disabled = false;
      scopeSel.value = 'GLOBAL';
    }
  }

  handleComunicadoScopeChange();
  document.getElementById('modalComunicadoTitulo').innerHTML = '<span class="material-icons-outlined">campaign</span> Nuevo Comunicado Institucional';
  document.getElementById('comunicadoModal').classList.remove('hidden');
}

function abrirModalComunicadoPrivado() {
  if (!currentStudentData) return;

  abrirModalNuevoComunicado();

  document.getElementById('comunicadoTargetStudentId').value = currentStudentData.id;
  document.getElementById('comunicadoCategory').value = 'PRIVADO';
  const scopeSel = document.getElementById('comunicadoScope');
  if (scopeSel) {
    scopeSel.value = 'PRIVATE_STUDENT';
  }
  handleComunicadoScopeChange();

  document.getElementById('modalComunicadoTitulo').innerHTML = `<span class="material-icons-outlined">mail</span> Mensaje Privado a Familia de ${currentStudentData.firstName}`;
}

function abrirModalEditarComunicado(comunicadoId) {
  const a = announcementsList.find(item => item.id === comunicadoId);
  if (!a) return;

  document.getElementById('comunicadoEditId').value = a.id;
  document.getElementById('comunicadoTargetStudentId').value = a.targetStudentId || '';
  document.getElementById('comunicadoTitle').value = a.title || '';
  document.getElementById('comunicadoCategory').value = a.category || 'GENERAL';
  document.getElementById('comunicadoScope').value = a.scope || 'GLOBAL';
  document.getElementById('comunicadoContent').value = a.content || '';
  document.getElementById('comunicadoMediaUrl').value = a.mediaUrl || '';

  handleComunicadoScopeChange();

  if (a.targetClassroom) {
    document.getElementById('comunicadoTargetClassroom').value = a.targetClassroom;
  }

  document.getElementById('modalComunicadoTitulo').innerHTML = '<span class="material-icons-outlined">edit</span> Editar Comunicado';
  document.getElementById('comunicadoModal').classList.remove('hidden');
}

function cerrarModalComunicado() {
  document.getElementById('comunicadoModal').classList.add('hidden');
}

async function guardarComunicado(e) {
  e.preventDefault();

  const editId = document.getElementById('comunicadoEditId').value.trim();
  const scope = document.getElementById('comunicadoScope').value;
  const targetStudentId = document.getElementById('comunicadoTargetStudentId').value.trim() || null;

  const payload = {
    authorId: currentSession.email,
    authorName: currentSession.email.split('@')[0],
    authorRole: currentSession.role,
    title: document.getElementById('comunicadoTitle').value.trim(),
    content: document.getElementById('comunicadoContent').value.trim(),
    category: document.getElementById('comunicadoCategory').value,
    scope: scope,
    targetClassroom: scope === 'CLASSROOM' ? document.getElementById('comunicadoTargetClassroom').value : null,
    targetStudentId: targetStudentId,
    mediaUrl: document.getElementById('comunicadoMediaUrl').value.trim() || null,
    isPinned: false
  };

  try {
    const url = editId
        ? `${API_BASE_URL}/announcements/${editId}`
        : `${API_BASE_URL}/announcements`;

    const method = editId ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method: method,
      headers: {
        'Content-Type': 'application/json',
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      alert("¡Comunicado procesado con éxito!");
      cerrarModalComunicado();
      await fetchAnnouncements();

      if (currentStudentData && !document.getElementById('studentProfileView').classList.contains('hidden')) {
        renderComunicadosPrivadosAlumno(currentStudentData.id);
      }
    } else {
      const err = await res.text();
      alert(`Error al guardar comunicado: ${err}`);
    }
  } catch (error) {
    console.error("Error al guardar comunicado:", error);
    alert("Error de conexión con el servidor.");
  }
}

async function eliminarComunicado(comunicadoId) {
  if (!confirm("¿Deseas eliminar este comunicado?")) return;

  try {
    const res = await fetch(`${API_BASE_URL}/announcements/${comunicadoId}`, {
      method: 'DELETE',
      headers: {
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      }
    });

    if (res.ok) {
      await fetchAnnouncements();
      if (currentStudentData) {
        renderComunicadosPrivadosAlumno(currentStudentData.id);
      }
    } else {
      alert("No se pudo eliminar el comunicado.");
    }
  } catch (error) {
    console.error("Error al eliminar comunicado:", error);
  }
}