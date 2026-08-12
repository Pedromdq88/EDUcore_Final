const API_BASE_URL = 'http://localhost:8080/api/v1';
let currentSession = { email: '', role: '', institutionId: '88888888-4444-4444-4444-121212121212' };

let activeTutors = [];
let activeStudents = [];
let activeStaff = [];

function handleLogin() {
  const email = document.getElementById('emailInput').value.trim();
  if (!email) return;
  
  let role = 'TEACHER';
  if (email.includes('direccion') || email.includes('director')) role = 'DIRECTOR';
  else if (email.includes('admin')) role = 'ADMINISTRATIVE';

  currentSession.email = email;
  currentSession.role = role;

  // Renderizar datos dinámicos requeridos en navbar
  document.getElementById('userDisplay').innerText = email;
  document.getElementById('roleBadge').innerText = role === 'DIRECTOR' ? 'Directora' : (role === 'ADMINISTRATIVE' ? 'Administrativo' : 'Docente');

  document.getElementById('loginPage').classList.add('hidden');
  document.getElementById('mainDashboard').classList.remove('hidden');
  
  refreshAllData();
}

function handleLogout() { location.reload(); }

function showSection(sectionId) {
  document.querySelectorAll('.dashboard-view').forEach(v => v.classList.add('hidden'));
  document.getElementById(sectionId).classList.remove('hidden');
  document.querySelectorAll('aside nav button').forEach(b => b.classList.remove('bg-emerald-50', 'text-emerald-700'));
  document.getElementById(`nav-${sectionId}`).classList.add('bg-emerald-50', 'text-emerald-700');
}

function toggleForm(id) { document.getElementById(id).classList.toggle('hidden'); }

function handleStaffRoleFormChange(role, targetBlockId) {
  const block = document.getElementById(targetBlockId);
  if (role === 'TEACHER') block.classList.remove('hidden');
  else block.classList.add('hidden');
}

function refreshAllData() { fetchTutors(); fetchStudents(); fetchStaff(); }

// ========================================================
// CAPA DE CONSULTAS Y ALTAS CON ACTUALIZACIÓN AUTOMÁTICA
// ========================================================

async function fetchTutors() {
  try {
    const r = await fetch(`${API_BASE_URL}/tutors`, { headers: { 'X-Institution-Id': currentSession.institutionId, 'X-User-Role': currentSession.role }});
    activeTutors = r.ok ? await r.json() : [];
    document.getElementById('countTutorsBadge').innerText = activeTutors.length;
    document.getElementById('tutorsTableBody').innerHTML = activeTutors.map(t => `
      <tr class="hover:bg-slate-50/80 transition-colors">
        <td class="p-3 font-semibold text-slate-900">${t.lastName}, ${t.firstName}</td>
        <td class="p-3 text-slate-600 font-medium">${t.documentNumber}</td>
        <td class="p-3"><span class="bg-amber-100 text-amber-800 text-xs font-bold px-2.5 py-1 rounded-md">${t.relationship}</span></td>
        <td class="p-3 text-center"><button onclick="viewTutorProfile('${t.id}')" class="text-slate-400 hover:text-emerald-600 p-1 rounded-full hover:bg-slate-100"><span class="material-icons-outlined">contact_page</span></button></td>
      </tr>
    `).join('');
  } catch(e) { console.error(e); }
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
  fetchTutors(); // 🔁 Refresh automático
}

function openStudentForm() {
  const select = document.getElementById('studentTutorSelect');
  select.innerHTML = '<option value="">-- Seleccionar Responsable --</option>' + activeTutors.map(t => `<option value="${t.id}">${t.lastName}, ${t.firstName} (${t.relationship})</option>`).join('');
  toggleForm('studentFormContainer');
}

// =========================================================================
// MÉTODOS DE ALUMNOS 
// =========================================================================

async function fetchStudents() {
  try {
    console.log("Intentando conectar con: " + `${API_BASE_URL}/students`);
    const r = await fetch(`${API_BASE_URL}/students`, { 
      headers: { 'X-Institution-Id': currentSession.institutionId, 'X-User-Role': currentSession.role }
    });
    
    if (!r.ok) {
      console.error("El backend respondió con código de error:", r.status);
    }

    activeStudents = r.ok ? await r.json() : [];
    console.log("Alumnos recuperados de MySQL:", activeStudents);
    renderStudentsTable(activeStudents);
  } catch(e) { 
    console.error("Se cortó la conexión física con StudentController:", e);
    renderStudentsTable([]); 
  }
}

function renderStudentsTable(list) {
  document.getElementById('countStudentsBadge').innerText = list.length;
  const tbody = document.getElementById('studentsTableBody');
  
  if (!list || list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" class="p-4 text-center text-slate-400 text-xs">No hay alumnos registrados o no se pudo conectar con el servidor.</td></tr>`;
    return;
  }

  tbody.innerHTML = list.map(s => `
    <tr class="hover:bg-slate-50/80 transition-colors">
      <td class="p-3 font-semibold text-slate-900">${s.lastName || ''}, ${s.firstName || ''}</td>
      <td class="p-3 text-slate-600 font-medium">${s.documentNumber || '--'}</td>
      <td class="p-3"><span class="bg-blue-50 text-blue-700 text-xs font-bold px-2.5 py-1 rounded-full">${s.classroom || 'Sin asignar'}</span></td>
      <td class="p-3 text-center">
        <button onclick="viewStudentProfile('${s.id}')" class="text-slate-400 hover:text-emerald-600 p-1 rounded-full hover:bg-slate-100">
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

async function submitStudent() {
  const birthDateValue = document.getElementById('studentBirthDate').value;
  
  if (!birthDateValue) {
    alert("La fecha de nacimiento es obligatoria para registrar la matrícula.");
    return;
  }

  // Estructura limpia y adaptada a StudentJpaEntity
  const studentData = {
    firstName: document.getElementById('studentFirstName').value.trim(),
    lastName: document.getElementById('studentLastName').value.trim(),
    documentNumber: document.getElementById('studentDni').value.trim(), 
    birthDate: birthDateValue,   
    classroom: document.getElementById('studentClassroom').value,
    direccion: document.getElementById('studentDireccion').value.trim(),
    telefonoContacto: document.getElementById('studentTelefono').value.trim(),
    status: "ACTIVE"
  };

  try {
    console.log("Enviando nuevo alumno...", studentData);
    const response = await fetch(`${API_BASE_URL}/students`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Institution-Id': currentSession.institutionId,
        'X-User-Role': currentSession.role
      },
      body: JSON.stringify(studentData)
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error("Rechazo de Java:", errorText);
      throw new Error("Error al guardar el alumno en el servidor");
    }

    const alumnoCreado = await response.json();
    console.log("Alumno creado con éxito en DB:", alumnoCreado);

    // Vinculamos al tutor mapeando el ID real retornado por la base de datos
    const tutorId = document.getElementById('studentTutorSelect').value; 
    if (tutorId && alumnoCreado.id) {
      console.log(`Asociando alumno ${alumnoCreado.id} con tutor ${tutorId}...`);
      const linkResponse = await fetch(`${API_BASE_URL}/students/${alumnoCreado.id}/tutors/${tutorId}`, {
        method: 'POST',
        headers: {
          'X-Institution-Id': currentSession.institutionId,
          'X-User-Role': currentSession.role
        }
      });
      
      if(linkResponse.ok) {
        alert("¡Matrícula y Tutor asignados correctamente!");
      }
    } else {
      alert("Matrícula creada sin tutor asignado.");
    }

    toggleForm('studentFormContainer');
    refreshAllData();

  } catch (error) {
    console.error("Explotó el alta de matrícula:", error);
    alert("No se pudo registrar la matrícula. Revisá las restricciones de Java.");
  }
}

async function fetchStaff() {
  try {
    const r = await fetch(`${API_BASE_URL}/institution/staff`, { headers: { 'X-Institution-Id': currentSession.institutionId, 'X-User-Role': currentSession.role }});
    activeStaff = r.ok ? await r.json() : [
      { id: "dir-1", firstName: "Alicia", lastName: "Fernández", email: "direccion@onceunidos.com", role: "DIRECTOR", hireDate: "2026-02-15", classroom: "" },
      { id: "staff-2", firstName: "Laura", lastName: "Sánchez", email: "laura@onceunidos.com", role: "TEACHER", hireDate: "2026-05-10", classroom: "Sala de 4" }
    ];
    renderStaffTable(activeStaff);
  } catch(e) {}
}

function renderStaffTable(list) {
  document.getElementById('countStaffBadge').innerText = list.length;
  document.getElementById('staffTableBody').innerHTML = list.map(u => `
    <tr class="hover:bg-slate-50/80 transition-colors">
      <td class="p-3 font-semibold text-slate-900">${u.lastName}, ${u.firstName}</td>
      <td class="p-3 text-slate-600 font-medium">${u.email}</td>
      <td class="p-3">
        <span class="bg-emerald-50 text-emerald-700 text-xs font-bold px-2 py-0.5 rounded">${u.role}</span>
        ${u.classroom ? `<span class="bg-blue-50 text-blue-700 text-[10px] font-bold px-1.5 py-0.5 rounded ml-1">${u.classroom}</span>` : ''}
      </td>
      <td class="p-3 text-xs text-slate-500">${u.hireDate || '--'}</td>
      <td class="p-3 text-center"><button onclick="viewStaffProfile('${u.id}')" class="text-slate-400 hover:text-emerald-600 p-1 rounded-full hover:bg-slate-100"><span class="material-icons-outlined">contact_page</span></button></td>
    </tr>
  `).join('');
}

async function submitStaff() {
  const role = document.getElementById('staffRole').value;
  const hireDateValue = document.getElementById('staffHireDate').value;

  if (!hireDateValue) {
    alert("La fecha de contratación es obligatoria.");
    return;
  }

  const payload = {
    id: crypto.randomUUID(), // 🟢 Genera un UUID único (ej: 'a1b2c3d4-...') para que Java no reciba un ID null
    firstName: document.getElementById('staffFirstName').value.trim(),
    lastName: document.getElementById('staffLastName').value.trim(),
    email: document.getElementById('staffEmail').value.trim(),
    password: "123",
    role: role,
    hireDate: hireDateValue,
    classroom: role === 'TEACHER' ? document.getElementById('staffAssignedClassroom').value : null,
    tenantId: currentSession.institutionId 
  };

  try {
    console.log("Enviando personal con ID automático...", payload);
    
    const response = await fetch(`${API_BASE_URL}/institution/staff`, {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json', 
        'X-Institution-Id': currentSession.institutionId, 
        'X-User-Role': currentSession.role 
      },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error("Rechazo del backend de Staff:", errorText);
      throw new Error(`Error en el servidor (Código ${response.status})`);
    }

    alert("¡Miembro del personal registrado correctamente!");
    toggleForm('staffFormContainer');
    fetchStaff(); // 🔁 Recarga la grilla activa de inmediato

  } catch (error) {
    console.error("Error en el alta de personal:", error);
    alert("Hubo un problema en el servidor al registrar al personal. Verificá que el email sea único.");
  }
}
// ========================================================
// LEGAJOS DIGITALES EDITABLES REALES
// ========================================================

function openProfileModal(title, subtitle, bodyHtml, actionsHtml) {
  document.getElementById('modalTitle').innerText = title;
  document.getElementById('modalSubtitle').innerText = subtitle;
  document.getElementById('modalBody').innerHTML = bodyHtml;
  document.getElementById('modalActions').innerHTML = actionsHtml;
  document.getElementById('profileModal').classList.remove('hidden');
}

function closeProfileModal() { document.getElementById('profileModal').classList.add('hidden'); }

function openMyProfile() {
  const isDirector = currentSession.role === 'DIRECTOR';
  const body = `
    <div><label class="modal-label">Mi Cuenta Escolar</label><input type="text" id="myEmail" value="${currentSession.email}" class="modal-input" ${!isDirector?'disabled':''}></div>
    <div><label class="modal-label">Jerarquía Autorizada</label><input type="text" value="${currentSession.role}" class="modal-input" disabled></div>
  `;
  const actions = `
    <button onclick="closeProfileModal()" class="px-4 py-2 border rounded-xl text-xs font-medium bg-white text-slate-600">Cerrar</button>
    <button onclick="saveMyProfileChanges()" class="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-xs">Guardar Cambios</button>
  `;
  openProfileModal("Mi Perfil Escolar", "Sesión Activa", body, actions);
}

function saveMyProfileChanges() {
  alert("Perfil actualizado correctamente en la sesión actual.");
  closeProfileModal();
}

function viewStaffProfile(id) {
  const u = activeStaff.find(item => item.id === id);
  if(!u) return;
  const isDirector = currentSession.role === 'DIRECTOR';

  const body = `
    <input type="hidden" id="editStaffId" value="${u.id}">
    <div><label class="modal-label">Nombre</label><input type="text" id="editStaffFirstName" value="${u.firstName}" class="modal-input" ${!isDirector?'disabled':''}></div>
    <div><label class="modal-label">Apellido</label><input type="text" id="editStaffLastName" value="${u.lastName}" class="modal-input" ${!isDirector?'disabled':''}></div>
    <div><label class="modal-label">Email Acceso</label><input type="email" id="editStaffEmail" value="${u.email}" class="modal-input" ${!isDirector?'disabled':''}></div>
    <div>
      <label class="modal-label">Cargo Administrativo</label>
      <select id="editStaffRole" onchange="handleStaffRoleFormChange(this.value, 'editClassroomBlock')" class="modal-input" ${!isDirector?'disabled':''}>
        <option value="TEACHER" ${u.role==='TEACHER'?'selected':''}>TEACHER</option>
        <option value="ADMINISTRATIVE" ${u.role==='ADMINISTRATIVE'?'selected':''}>ADMINISTRATIVE</option>
        <option value="DIRECTOR" ${u.role==='DIRECTOR'?'selected':''}>DIRECTOR</option>
      </select>
    </div>
    <div id="editClassroomBlock" class="${u.role==='TEACHER'?'':'hidden'} bg-emerald-50 p-2.5 rounded-lg mt-2">
      <label class="modal-label text-emerald-800">Salita Asignada</label>
      <select id="editStaffClassroom" class="modal-input">
        <option value="Maternal" ${u.classroom==='Maternal'?'selected':''}>Maternal</option>
        <option value="Sala de 2 y 3" ${u.classroom==='Sala de 2 y 3'?'selected':''}>Sala de 2 y 3</option>
        <option value="Sala de 4" ${u.classroom==='Sala de 4'?'selected':''}>Sala de 4</option>
        <option value="Sala de 5" ${u.classroom==='Sala de 5'?'selected':''}>Sala de 5</option>
      </select>
    </div>
  `;

  let actions = `<button onclick="closeProfileModal()" class="px-4 py-2 border rounded-xl text-xs bg-white text-slate-600">Cerrar</button>`;
  if(isDirector) {
    actions += `<button onclick="saveStaffProfileChanges()" class="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-xs">Guardar Cambios</button>`;
    actions += `<button onclick="deleteStaffFromServer('${u.id}')" class="px-3 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl flex items-center gap-0.5"><span class="material-icons-outlined text-sm">delete</span> Dar de Baja</button>`;
  }

  openProfileModal(`${u.lastName}, ${u.firstName}`, "Perfil Modificable", body, actions);
}

async function saveStaffProfileChanges() {
  const id = document.getElementById('editStaffId').value;
  const role = document.getElementById('editStaffRole').value;
  const updated = {
    firstName: document.getElementById('editStaffFirstName').value,
    lastName: document.getElementById('editStaffLastName').value,
    email: document.getElementById('editStaffEmail').value,
    role: role,
    classroom: role === 'TEACHER' ? document.getElementById('editStaffClassroom').value : null
  };
  
  await fetch(`${API_BASE_URL}/institution/staff/${id}`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json', 'X-Institution-Id': currentSession.institutionId, 'X-User-Role': currentSession.role },
    body: JSON.stringify(updated)
  });
  closeProfileModal();
  fetchStaff();
}

async function deleteStudentFromServer(id) {
  if(!confirm("¿Confirmás el trámite de baja definitiva para este alumno? Se archivará con la fecha de hoy.")) return;
  try {
    await fetch(`${API_BASE_URL}/students/${id}/baja`, { 
      method: 'POST', 
      headers: { 'X-Institution-Id': currentSession.institutionId, 'X-User-Role': currentSession.role }
    });
    alert("La matrícula ha sido dada de baja y trasladada al registro histórico correctamente.");
    closeProfileModal();
    fetchStudents();
  } catch (error) { alert("Error al procesar la baja."); }
}

async function deleteStaffFromServer(id) {
  if(!confirm("¿Confirmás la baja de este miembro del personal? Dejará de formar parte del jardín activo.")) return;
  try {
    await fetch(`${API_BASE_URL}/institution/staff/${id}/baja`, { 
      method: 'POST', 
      headers: { 'X-Institution-Id': currentSession.institutionId, 'X-User-Role': currentSession.role }
    });
    alert("El usuario ha sido desvinculado de la institución activa.");
    closeProfileModal();
    fetchStaff();
  } catch(e) {}
}

async function deleteTutorFromServer(id) {
  if(!confirm("¿Desvincular a este adulto responsable? ADVERTENCIA: Si es el único tutor de un alumno, la matrícula del menor será dada de baja automáticamente por seguridad.")) return;
  try {
    const response = await fetch(`${API_BASE_URL}/tutors/${id}/baja`, { 
      method: 'POST', 
      headers: { 'X-Institution-Id': currentSession.institutionId, 'X-User-Role': currentSession.role }
    });

    if(response.ok) {
      alert("Responsable familiar removido. Se revisaron las matrículas de los menores asociados y se archivaron las bajas correspondientes.");
      closeProfileModal();
      refreshAllData(); 
    } else {
      alert("El servidor rechazó la baja del tutor.");
    }
  } catch (e) {
    console.error(e);
  }
}

function viewStudentProfile(id) {
  const s = activeStudents.find(item => item.id === id);
  if(!s) return;
  const isDirector = currentSession.role === 'DIRECTOR' || currentSession.role === 'ADMINISTRATIVE';

  const body = `
    <div><span class="font-bold text-slate-500">DNI:</span> ${s.documentNumber}</div>
    <div><span class="font-bold text-slate-500">Salita Actual:</span> ${s.classroom}</div>
    <div><span class="font-bold text-slate-500">Fecha de Nacimiento:</span> ${s.birthDate}</div>
    <div><span class="font-bold text-slate-500">Dirección Habitacional:</span> ${s.direccion || '--'}</div>
  `;

  let actions = `<button onclick="closeProfileModal()" class="px-4 py-2 border rounded-xl text-xs bg-white text-slate-600">Cancelar</button>`;
  if(isDirector) {
    actions += `<button onclick="deleteStudentFromServer('${s.id}')" class="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl flex items-center gap-1 cursor-pointer"><span class="material-icons-outlined text-sm">remove_circle</span> Dar de Baja (Retirar Matrícula)</button>`;
  }
  openProfileModal(`${s.lastName}, ${s.firstName}`, "Legajo Digital del Alumno", body, actions);
}

function viewTutorProfile(id) {
  const t = activeTutors.find(item => item.id === id);
  if(!t) return;
  const isDirector = currentSession.role === 'DIRECTOR' || currentSession.role === 'ADMINISTRATIVE';

  const body = `
    <div><span class="font-bold text-slate-500">DNI:</span> ${t.documentNumber}</div>
    <div><span class="font-bold text-slate-500">Parentesco:</span> ${t.relationship}</div>
    <div><span class="font-bold text-slate-500">Email:</span> ${t.email || '--'}</div>
    <div><span class="font-bold text-slate-500">Teléfono:</span> ${t.phone || '--'}</div>
  `;
  
  let actions = `<button onclick="closeProfileModal()" class="px-4 py-2 border rounded-xl text-xs bg-white text-slate-600">Cerrar</button>`;
  if(isDirector) {
    actions += `<button onclick="deleteTutorFromServer('${t.id}')" class="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl flex items-center gap-1 cursor-pointer"><span class="material-icons-outlined text-sm">delete</span> Dar de Baja</button>`;
  }
  openProfileModal(`${t.lastName}, ${t.firstName}`, "Legajo del Adulto", body, actions);
}