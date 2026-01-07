import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import loanService from "../services/loan.service";
import {
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    Button,
    Chip,
    Box,
    Typography
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import AssignmentTurnedInIcon from '@mui/icons-material/AssignmentTurnedIn';
import ReturnLoanModal from './ReturnLoanModal';
import { useKeycloak } from "@react-keycloak/web";

const LoanList = () => {
    const [loans, setLoans] = useState([]);
    const navigate = useNavigate();
    const { keycloak, initialized } = useKeycloak();

    const [isModalOpen, setModalOpen] = useState(false);
    const [selectedLoanId, setSelectedLoanId] = useState(null);
    
    const isAdmin = initialized && keycloak.hasRealmRole("ADMIN");

    const fetchLoans = () => {
        if (!initialized || !keycloak.authenticated) return;

        if (isAdmin) {
            loanService.getAll()
                .then((response) => setLoans(response.data))
                .catch((error) => console.log("Error al mostrar todos los préstamos.", error));
        } else {
            loanService.getMyLoans()
                .then((response) => setLoans(response.data))
                .catch((error) => console.log("Error al mostrar mis préstamos.", error));
        }
    };

    useEffect(() => {
        fetchLoans();
    }, [initialized, keycloak.authenticated]);

    const handleDelete = (id) => {
        if (window.confirm("¿Está seguro que desea borrar este préstamo?")) {
            loanService.remove(id)
                .then(() => fetchLoans())
                .catch((error) => alert(`Error: ${error.response?.data || "No se pudo eliminar."}`));
        }
    };

    const handleEdit = (id) => {
        navigate(`/loans/edit/${id}`);
    };

    const handleOpenReturnModal = (id) => {
        setSelectedLoanId(id);
        setModalOpen(true);
    };

    const handleCloseModal = () => {
        setModalOpen(false);
        setSelectedLoanId(null);
    };

    const handleConfirmReturn = (toolStatus) => {
        if (selectedLoanId) {
            loanService.processReturn(selectedLoanId, { status: toolStatus })
                .then(() => {
                    alert("Devolución registrada exitosamente.");
                    fetchLoans();
                })
                .catch(error => alert(`Error: ${error.response?.data?.message || "Ocurrió un error."}`));
        }
    };

    if (!initialized) {
        return <Typography sx={{ margin: 2 }}>Cargando autenticación...</Typography>;
    }

    return (
        <>
            <Box sx={{ margin: 2 }}>
                <Link to="/loans/add" style={{ textDecoration: "none" }}>
                    <Button variant="contained" color="primary" startIcon={<AssignmentTurnedInIcon />}>
                        Añadir Préstamo
                    </Button>
                </Link>
            </Box>
            <TableContainer component={Paper} sx={{ margin: 2, width: 'auto' }}>
                <Table>
                    <TableHead>
                        <TableRow>
                            {isAdmin && <TableCell sx={{ fontWeight: "bold" }}>Cliente</TableCell>}
                            <TableCell sx={{ fontWeight: "bold" }}>Herramienta</TableCell>
                            <TableCell sx={{ fontWeight: "bold" }}>Fecha Préstamo</TableCell>
                            <TableCell sx={{ fontWeight: "bold" }}>Fecha Límite</TableCell>
                            <TableCell sx={{ fontWeight: "bold" }}>Fecha Devolución</TableCell>
                            <TableCell sx={{ fontWeight: "bold" }}>Estado</TableCell>
                            <TableCell sx={{ fontWeight: "bold" }}>Multa</TableCell>
                            <TableCell sx={{ fontWeight: "bold" }}>Acciones</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {loans.map((loan) => (
                            <TableRow key={loan.id}>
                                {isAdmin && <TableCell>{loan.clientName}</TableCell>}
                                <TableCell>{loan.toolName}</TableCell>
                                <TableCell>{loan.loanDate}</TableCell>
                                <TableCell>{loan.dueDate}</TableCell>
                                <TableCell>{loan.returnDate || "Pendiente"}</TableCell>
                                <TableCell>
                                    <Chip label={loan.status} color={loan.status === 'Activo' ? 'primary' : 'default'} size="small" />
                                </TableCell>
                                <TableCell>
                                    {loan.fineStatus ? (
                                        <Chip label={`$${loan.fineAmount} - ${loan.fineStatus}`} color={loan.fineStatus === 'Pendiente' ? 'error' : 'success'} size="small" />
                                    ) : "Sin multa"}
                                </TableCell>
                                <TableCell>
                                    {/* */}
                                    {isAdmin && (
                                        <>
                                            <Button variant="contained" color="info" size="small" onClick={() => handleEdit(loan.id)} startIcon={<EditIcon />}>Editar</Button>
                                            <Button variant="contained" color="error" size="small" onClick={() => handleDelete(loan.id)} style={{ marginLeft: "0.5rem" }} startIcon={<DeleteIcon />}>Borrar</Button>
                                        </>
                                    )}
                                    
                                    {/*  */}
                                    {loan.status === 'Activo' && (
                                        <Button variant="contained" color="success" size="small" onClick={() => handleOpenReturnModal(loan.id)} style={{ marginLeft: "0.5rem" }}>Devolver</Button>
                                    )}
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            <ReturnLoanModal open={isModalOpen} onClose={handleCloseModal} onSubmit={handleConfirmReturn} />
        </>
    );
};

export default LoanList;