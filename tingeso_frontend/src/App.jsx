import './App.css'
import {BrowserRouter as Router, Route, Routes} from 'react-router-dom'
import Navbar from "./components/Navbar"
import Home from './components/Home';
import NotFound from './components/NotFound';
import AddEditTool from './components/addEditTool';
import ToolList from './components/ToolList';
import LoanList from './components/LoanList';
import AddEditLoan from './components/addEditLoan';
import ClientList from './components/ClientList';
import AddEditClient from './components/addEditClient';
import FineList from './components/FineList';
import ManageFees from './components/ManageFees';
import KardexView from './components/KardexView';
import { useKeycloak } from "@react-keycloak/web";

function App() {
  const { keycloak, initialized } = useKeycloak();
  if (!initialized) return <div>Cargando...</div>;

  const isLoggedIn = keycloak.authenticated;
  const roles = keycloak.tokenParsed?.realm_access?.roles || [];

  const PrivateRoute = ({ element, rolesAllowed }) => {
    if (!isLoggedIn) {
      keycloak.login();
      return null;
    }
    if (rolesAllowed && !rolesAllowed.some(r => roles.includes(r))) {
      return <h2>No tienes permiso para ver esta página</h2>;
    }
    return element;
  };

  if (!isLoggedIn) { 
    keycloak.login(); 
    return null; 
  }  

  return (
      <Router>
          <div className="container">
          <Navbar></Navbar>
            <Routes>
              <Route path="/home" element={<Home/>} />
              <Route path="/tools/add" element={<PrivateRoute element={<AddEditTool/>} rolesAllowed={["USER","ADMIN"]} />} />
              <Route path="/tools/edit/:id" element={<PrivateRoute element={<AddEditTool/>} rolesAllowed={["USER","ADMIN"]} />} />
              <Route path="/tools" element={<PrivateRoute element={<ToolList/>} rolesAllowed={["USER","ADMIN"]} />} />
              <Route path="/loans" element={<PrivateRoute element={<LoanList/>} rolesAllowed={["USER","ADMIN"]} />} />
              <Route path="/loans/add" element={<PrivateRoute element={<AddEditLoan/>} rolesAllowed={["USER","ADMIN"]} />} />
              <Route path="/loans/edit/:id" element={<PrivateRoute element={<AddEditLoan/>} rolesAllowed={["ADMIN"]} />} />
              <Route path="/clients" element={<PrivateRoute element={<ClientList/>} rolesAllowed={["ADMIN"]} />} />
              <Route path="/clients/add" element={<PrivateRoute element={<AddEditClient/>} rolesAllowed={["ADMIN"]} />} />
              <Route path="/clients/edit/:id" element={<PrivateRoute element={<AddEditClient/>} rolesAllowed={["ADMIN"]} />} />
              <Route path="/fines" element={<PrivateRoute element={<FineList/>} rolesAllowed={["USER","ADMIN"]} />} />
              <Route path="/fees" element={<PrivateRoute element={<ManageFees/>} rolesAllowed={["USER","ADMIN"]} />} />
              <Route path="/kardex" element={<PrivateRoute element={<KardexView/>} rolesAllowed={["ADMIN"]} />} />
              <Route path="/profile/edit" element={<AddEditClient />} />
              <Route path="*" element={<Home/>} />
            </Routes>
          </div>
      </Router>
  );
}

export default App
