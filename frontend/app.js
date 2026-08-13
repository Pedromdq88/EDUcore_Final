const API_BASE_URL = 'http://localhost:8080/api/v1';
let currentSession = { email: '', role: '', institutionId: '88888888-4444-4444-4444-121212121212' };

let activeTutors = [];
let activeStudents = [];
let activeStaff = [];

let currentTutorData = null;
let currentStaffData = null;
let currentEditingStudentId = null;

// ========================================================
// AUTENTICACIÓN Y NAVEGACIÓN
// ========================================================

function handleLogin() {
  const email = document.getElementById('emailInput').value.trim();
  if (!email) return;

  let role = 'TEACHER';
  if (email.includes('direccion') || email.includes('director')) role = 'DIRECTOR';
  else if (email.includes('admin')) role = 'ADMINISTRATIVE';
  else if (email.includes('tutor') || email.includes('padre')) role = 'TUTOR';

  currentSession.email = email;
  currentSession.role = role;

  document.getElementById('userDisplay').innerText = email;
  document.getElementById('roleBadge').innerText =
      role === 'DIRECTOR' ? 'Directora' :
          (role === 'ADMINISTRATIVE' ? 'Administrativo' :
              (role === 'TUTOR' ? 'Tutor' : 'Docente'));

  document.getElementById('loginPage').classList.add('hidden');
  document.getElementById('mainDashboard').classList.remove('hidden');

  refreshAllData();
}

function handleLogout() { location.reload(); }

function showSection(sectionId) {
  document.querySelectorAll('.dashboard-view').forEach(v => v.classList.add('hidden'));
  document.getElementById(sectionId).classList.remove('hidden');
  document.querySelectorAll('aside nav button').forEach(b => b.classList.remove('bg-emerald-50', 'text-emerald-700'));
  const btn = document.getElementById(`nav-${sectionId}`);
  if (btn) btn.classList.add('bg-emerald-50', 'text-emerald-700');
}

function toggleForm(id) { document.getElementById(id).classList.toggle('hidden'); }

function refreshAllData() { fetchTutors(); fetchStudents(); fetchStaff(); }

function getInitials(str) {
  if (!str) return '--';
  return str.split(' ').filter(Boolean).map(n => n[0]).join('').substring(0, 2).toUpperCase() || '--';
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
          <button onclick="viewTutorProfile('${t.id}')" class="text-slate-400 hover:text-emerald-600 p-1 rounded-full hover:bg-slate-100">
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

function viewTutorProfile(id) {
  const t = activeTutors.find(item => item.id === id);
  if(!t) {
    alert("No se encontraron los datos de este tutor.");
    return;
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
    hijos: activeStudents.filter(s => s.tutorIds && s.tutorIds.includes(t.id)).map(s => ({
      id: s.id,
      nombre: s.firstName,
      apellido: s.lastName,
      curso: s.classroom,
      parentesco: 'Hijo/a'
    }))
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
  showSection('tutorsView');
}

function evaluarPermisosPerfilTutor() {
  const btnEdit = document.getElementById('btn-edit-tutor');
  const btnBaja = document.getElementById('btn-baja-tutor');
  const role = currentSession.role;

  if (btnEdit) {
    if (['DIRECTOR', 'ADMINISTRATIVE', 'TEACHER', 'TUTOR'].includes(role)) {
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

    if (response.ok) {
      alert("¡Datos del tutor actualizados con éxito!");
      cerrarModalEdicionTutor();
      fetchTutors();
    } else {
      setTutorData(updatedData);
      cerrarModalEdicionTutor();
    }
  } catch(error) {
    setTutorData(updatedData);
    cerrarModalEdicionTutor();
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
        <button onclick="showStudentProfile('${s.id}')" class="text-slate-400 hover:text-emerald-600 p-1 rounded-full hover:bg-slate-100">
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

async function showStudentProfile(studentId) {
  try {
    const response = await fetch(`${API_BASE_URL}/students/${studentId}`, {
      headers: { 'X-Institution-Id': currentSession.institutionId, 'X-User-Role': currentSession.role }
    });

    if (!response.ok) throw new Error("Error al consultar el perfil");
    const data = await response.json();

    document.querySelectorAll('.dashboard-view').forEach(view => view.classList.add('hidden'));
    document.getElementById('studentProfileView').classList.remove('hidden');

    const nombre = data.firstName || '';
    const apellido = data.lastName || '';
    document.getElementById('alumno-nombre').textContent = `${nombre} ${apellido}`.trim() || '-';
    document.getElementById('alumno-avatar').textContent = getInitials(`${nombre} ${apellido}`);

    document.getElementById('alumno-legajo').textContent = `Legajo: ${data.id || '-'}`;
    document.getElementById('alumno-curso').textContent = `Salita: ${data.classroom || '-'}`;
    document.getElementById('alumno-estado').textContent = `Estado: ${data.status || 'ACTIVO'}`;
    document.getElementById('alumno-dni').textContent = data.documentNumber || '-';
    document.getElementById('alumno-nacimiento').textContent = data.birthDate || '-';
    document.getElementById('alumno-domicilio').textContent = data.direccion || '-';

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
        card.onclick = () => viewTutorProfile(t.id || t.tutorId);

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

  } catch (error) {
    console.error("Error al cargar perfil de alumno:", error);
    alert("No se pudo cargar la ficha del alumno.");
  }
}

function hideStudentProfile() {
  document.getElementById('studentProfileView').classList.add('hidden');
  document.getElementById('alumnosView').classList.remove('hidden');
  fetchStudents();
}

async function confirmarBajaAlumno() {
  const legajoText = document.getElementById('alumno-legajo').textContent;
  const studentId = legajoText.replace('Legajo: ', '').trim();

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

  const studentData = {
    firstName: document.getElementById('studentFirstName')?.value.trim() || '',
    lastName: document.getElementById('studentLastName')?.value.trim() || '',
    documentNumber: document.getElementById('studentDni')?.value.trim() || '',
    birthDate: birthDateValue,
    classroom: document.getElementById('studentClassroom')?.value || '',
    direccion: document.getElementById('studentDireccion')?.value.trim() || '',
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