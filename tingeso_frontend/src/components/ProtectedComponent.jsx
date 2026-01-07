// ProtectedComponent.jsx
import { useKeycloak } from '@react-keycloak/web';

const ProtectedComponent = ({ rolesAllowed, children }) => {
  const { keycloak } = useKeycloak();

  // Función para verificar si el usuario tiene al menos uno de los roles permitidos
  const isAuthorized = () => {
    if (keycloak && rolesAllowed) {
      // some() devuelve true si al menos un elemento del array cumple la condición
      return rolesAllowed.some(role => {
        const userRoles = keycloak.tokenParsed?.realm_access?.roles || [];
        return userRoles.includes(role);
      });
    }
    return false;
  };

  // Si está autorizado, renderiza los componentes hijos, si no, no renderiza nada.
  return isAuthorized() ? <>{children}</> : null;
};

export default ProtectedComponent;