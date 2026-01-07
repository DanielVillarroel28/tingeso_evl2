import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import ClientService from "../services/client.service";
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Button, Chip } from "@mui/material";
import PersonAddIcon from "@mui/icons-material/PersonAdd";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import { useKeycloak } from "@react-keycloak/web";

const ClientList = () => {
  const [clients, setClients] = useState([]);
  const navigate = useNavigate();
  const { keycloak, initialized } = useKeycloak();

  const init = () => {
    ClientService.getAll()
      .then((response) => setClients(response.data))
      .catch((error) => console.log("Error al mostrar clientes.", error));
  };

  useEffect(() => { init(); }, []);

  const handleDelete = (id) => {
    if (window.confirm("¿Está seguro que desea borrar este cliente?")) {
      ClientService.remove(id)
        .then(() => init())
        .catch((error) => console.log("Error al eliminar el cliente", error));
    }
  };

  const handleEdit = (id) => {
    navigate(`/clients/edit/${id}`);
  };

  return (
    <TableContainer component={Paper}>
      <br />
      <Link to="/clients/add" style={{ textDecoration: "none", margin: "1rem" }}>
        <Button variant="contained" color="primary" startIcon={<PersonAddIcon />}>
          Añadir Cliente
        </Button>
      </Link>
      <br /> <br />
      <Table>
        <TableHead>
          <TableRow>
            <TableCell sx={{ fontWeight: "bold" }}>Nombre</TableCell>
            <TableCell sx={{ fontWeight: "bold" }}>Rut</TableCell>
            <TableCell sx={{ fontWeight: "bold" }}>Teléfono</TableCell>
            <TableCell sx={{ fontWeight: "bold" }}>Email</TableCell>
            <TableCell sx={{ fontWeight: "bold" }}>Estado</TableCell>
            <TableCell sx={{ fontWeight: "bold" }}>Acciones</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {clients.map((client) => (
            <TableRow key={client.id}>
              <TableCell>{client.name}</TableCell>
              <TableCell>{client.rut}</TableCell>
              <TableCell>{client.phone}</TableCell>
              <TableCell>{client.email}</TableCell>
              <TableCell>
                <Chip
                  label={client.status}
                  color={client.status === 'Activo' ? 'success' : 'error'}
                  size="small"
                />
              </TableCell>
              <TableCell>

    <Button 
      variant="contained" 
      color="info" 
      size="small" 
      onClick={() => handleEdit(client.id)} 
      startIcon={<EditIcon />}
    >
      Editar
    </Button>



    <Button 
      variant="contained" 
      color="error" 
      size="small" 
      onClick={() => handleDelete(client.id)} 
      style={{ marginLeft: "0.5rem" }} 
      startIcon={<DeleteIcon />}
    >
      Eliminar
    </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};

export default ClientList;