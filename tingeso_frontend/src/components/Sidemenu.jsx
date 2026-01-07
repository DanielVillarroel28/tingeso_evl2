import * as React from "react";
import { useNavigate } from "react-router-dom";
import { useKeycloak } from "@react-keycloak/web";
import Box from "@mui/material/Box";
import Drawer from "@mui/material/Drawer";
import List from "@mui/material/List";
import Divider from "@mui/material/Divider";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemIcon from "@mui/material/ListItemIcon";
import ListItemText from "@mui/material/ListItemText";
import HomeIcon from "@mui/icons-material/Home";
import HandymanIcon from '@mui/icons-material/Handyman';
import MoreTimeIcon from "@mui/icons-material/MoreTime";
import PaidIcon from "@mui/icons-material/Paid";
import CalculateIcon from "@mui/icons-material/Calculate";
import AnalyticsIcon from "@mui/icons-material/Analytics";
import DiscountIcon from "@mui/icons-material/Discount";
import HailIcon from "@mui/icons-material/Hail";
import MedicationLiquidIcon from "@mui/icons-material/MedicationLiquid";


export default function Sidemenu({ open, toggleDrawer }) {
  const navigate = useNavigate();
  const { keycloak, initialized } = useKeycloak();

  if (!initialized) {
    return null; 
  }

  const listOptions = () => (
    <Box
      sx={{ width: 250 }} 
      role="presentation"
      onClick={toggleDrawer(false)}
    >
      <List>
        <ListItemButton onClick={() => navigate("/home")}>
          <ListItemIcon><HomeIcon /></ListItemIcon>
          <ListItemText primary="Home" />
        </ListItemButton>

        <Divider />

        <ListItemButton onClick={() => navigate("/tools")}>
          <ListItemIcon><HandymanIcon /></ListItemIcon>
          <ListItemText primary="Herramientas" />
        </ListItemButton>

        <ListItemButton onClick={() => navigate("/loans")}>
          <ListItemIcon><MoreTimeIcon /></ListItemIcon>
          <ListItemText primary="Prestamos y Devoluciones" />
        </ListItemButton>

        {keycloak.tokenParsed?.realm_access?.roles?.includes("ADMIN") && (
        <ListItemButton onClick={() => navigate("/clients")}>
          <ListItemIcon><PaidIcon /></ListItemIcon>
          <ListItemText primary="Clientes" />
        </ListItemButton>)}

        <ListItemButton onClick={() => navigate("/fines")}>
          <ListItemIcon><CalculateIcon /></ListItemIcon>
          <ListItemText primary="Tarifas y multas" />
        </ListItemButton>

        {/* Solo visible para el rol "ADMIN" */}
        {keycloak.tokenParsed?.realm_access?.roles?.includes("ADMIN") && (
          <ListItemButton onClick={() => navigate("/fees")}>
            <ListItemIcon><AnalyticsIcon /></ListItemIcon>
            <ListItemText primary="Configuración Tarifas" />
          </ListItemButton>
        )}
      </List>

      <Divider />

      <List>
        {/* Solo visible para el rol "ADMIN" */}
        {keycloak.tokenParsed?.realm_access?.roles?.includes("ADMIN") && (
          <ListItemButton onClick={() => navigate("/kardex")}>
            <ListItemIcon><DiscountIcon /></ListItemIcon>
            <ListItemText primary="Kardex" />
          </ListItemButton>
        )}
      </List>
    </Box>
  );

  return (
    <Drawer anchor={"left"} open={open} onClose={toggleDrawer(false)}>
      {listOptions()}
    </Drawer>
  );
}