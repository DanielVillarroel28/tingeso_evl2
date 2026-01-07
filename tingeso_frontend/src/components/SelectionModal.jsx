import {
    Dialog,
    DialogTitle,
    DialogContent,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    TextField,
    Box
} from '@mui/material';
import { useState } from 'react';

const SelectionModal = ({ open, onClose, onSelect, data, title, columns }) => {
    const [filter, setFilter] = useState('');

    const filteredData = data.filter(item =>
        columns.some(col =>
            String(item[col.field]).toLowerCase().includes(filter.toLowerCase())
        )
    );

    const handleSelect = (item) => {
        onSelect(item);
        onClose();
    };

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
            <DialogTitle>{title}</DialogTitle>
            <DialogContent>
                <Box sx={{ my: 2 }}>
                    <TextField
                        fullWidth
                        label="Buscar..."
                        variant="outlined"
                        value={filter}
                        onChange={(e) => setFilter(e.target.value)}
                    />
                </Box>
                <TableContainer component={Paper}>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                {columns.map((col) => (
                                    <TableCell key={col.field} sx={{ fontWeight: 'bold' }}>
                                        {col.headerName}
                                    </TableCell>
                                ))}
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {filteredData.map((item) => (
                                <TableRow
                                    key={item.id}
                                    hover
                                    onClick={() => handleSelect(item)}
                                    style={{ cursor: 'pointer' }}
                                >
                                    {columns.map((col) => (
                                        <TableCell key={col.field}>{item[col.field]}</TableCell>
                                    ))}
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            </DialogContent>
        </Dialog>
    );
};

export default SelectionModal;