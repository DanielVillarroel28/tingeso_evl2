import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import toolService from "../services/tool.service";
import {
    Box,
    Collapse,
    IconButton,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    Button,
    Typography
} from "@mui/material";
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import { useKeycloak } from "@react-keycloak/web"; 


function Row({ groupName, instances, handleEdit, handleDelete }) {
  const [open, setOpen] = useState(false);
  
  const totalStock = instances.length;
  const availableStock = instances.filter(i => i.status === 'Disponible').length;
  const category = instances[0]?.category;

  return (
    <>
      <TableRow sx={{ '& > *': { borderBottom: 'unset' } }}>
        <TableCell>
          <IconButton aria-label="expand row" size="small" onClick={() => setOpen(!open)}>
            {open ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
          </IconButton>
        </TableCell>
        <TableCell component="th" scope="row">{groupName}</TableCell>
        <TableCell>{category}</TableCell>
        <TableCell align="right">{availableStock} de {totalStock}</TableCell>
      </TableRow>
      <TableRow>
        <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={4}>
          <Collapse in={open} timeout="auto" unmountOnExit>
            <Box sx={{ margin: 1 }}>
              <Typography variant="h6" gutterBottom component="div">
                Detalle de Unidades
              </Typography>
              <Table size="small" aria-label="instances">
                <TableHead>
                  <TableRow>
                    <TableCell>ID de Unidad</TableCell>
                    <TableCell>Estado</TableCell>
                    <TableCell>Acciones</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {instances.map((instance) => (
                    <TableRow key={instance.id}>
                      <TableCell>{instance.id}</TableCell>
                      <TableCell>{instance.status}</TableCell>
                      <TableCell>
                        <Button
                          variant="contained"
                          color="info"
                          size="small"
                          onClick={() => handleEdit(instance.id)}
                          startIcon={<EditIcon />}
                        >
                          Editar
                        </Button>
                        <Button
                          variant="contained"
                          color="error"
                          size="small"
                          onClick={() => handleDelete(instance.id)}
                          style={{ marginLeft: "0.5rem" }}
                          startIcon={<DeleteIcon />}
                        >
                          Dar de baja
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>
    </>
  );
}

const ToolList = () => {
    const [groupedTools, setGroupedTools] = useState([]);
    const navigate = useNavigate();
    const { keycloak, initialized } = useKeycloak(); 

    const isAdmin = initialized && keycloak.hasRealmRole("ADMIN");

    const init = () => {
        toolService.getAll().then(response => {
            const allTools = response.data;
            const groups = allTools.reduce((acc, tool) => {
                const key = `${tool.name.toUpperCase()}-${tool.category.toUpperCase()}`;
                if (!acc[key]) {
                    acc[key] = [];
                }
                acc[key].push(tool);
                return acc;
            }, {});
            setGroupedTools(Object.entries(groups));
        });
    };

    useEffect(() => {
        init();
    }, []);

    const handleEdit = (id) => {
        navigate(`/tools/edit/${id}`);
    };

    const handleDelete = (id) => {
        const confirmDelete = window.confirm("¿Está seguro que desea dar de baja esta unidad?");
        if (confirmDelete) {
            toolService.remove(id)
                .then(() => {
                    console.log("Unidad de herramienta dada de baja.");
                    init();
                })
                .catch(error => {
                    console.error("Error al dar de baja la unidad.", error);
                });
        }
    };
    
    if (!initialized) {
        return <Typography sx={{ margin: 2 }}>Cargando...</Typography>;
    }

    return (
        <Box sx={{ margin: 2 }}>
            {keycloak.hasRealmRole("ADMIN") && (
                <Link to="/tools/add" style={{ textDecoration: "none" }}>
                    <Button variant="contained" color="primary" startIcon={<AddIcon />}>
                        Añadir Nueva Herramienta
                    </Button>
                </Link>
            )}
            

            <TableContainer component={Paper} sx={{ mt: 2 }}>
                <Table aria-label="collapsible table">
                    <TableHead>
                        <TableRow>
                            <TableCell />
                            <TableCell sx={{ fontWeight: 'bold' }}>Nombre Herramienta</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Categoría</TableCell>
                            <TableCell align="right" sx={{ fontWeight: 'bold' }}>Stock Disponible</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {groupedTools.map(([key, instances]) => (
                            <Row 
                              key={key} 
                              groupName={instances[0].name}
                              instances={instances}
                              handleEdit={handleEdit}
                              handleDelete={handleDelete}
                            />
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
};

export default ToolList;